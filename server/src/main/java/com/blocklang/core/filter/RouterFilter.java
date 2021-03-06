package com.blocklang.core.filter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

import com.blocklang.core.constant.WebSite;
import com.blocklang.core.controller.RequestUtil;


/**
 * URL 转换过滤器
 * 
 * <p>
 * 将用户友好的 URL 转换为 RESTful 风格的 URL。 如将 "/{owner}/{projectName}" 转换为
 * "/projects/{owner}/{projectName}" 
 * 注意：转换的经过 forward 跳转的 url 不会被 security
 * 拦截，即判断用户是否登录的权限控制需要单独处理
 * 
 * @author jinzw
 *
 */
// 已在 SecurityConfig 中手动注入该 Filter，在此处不需使用 @Component("routerFilter") 方式注入
public class RouterFilter implements Filter{

	private Map<String, String> sourceMap;
	private List<UriTemplate> routerTemplates;
	
	// 因为在 Spring boot 中不会执行 Filter#init 和 Filter#destroy 方法
	// 所以将初始化操作放在构造函数中。
	public RouterFilter() {
		sourceMap = new HashMap<String, String>();
		routerTemplates = Arrays.stream(Resources.ROUTES).map(route -> {
			return new UriTemplate(route);
		}).collect(Collectors.toList());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		// 如果是 fetch 请求，则不做任何处理
		if(RequestUtil.isFetch(httpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}
		
		// 如果是从 git 客户端发出的请求，则不做任何处理
		if(RequestUtil.isGit(httpServletRequest)) {
			httpServletRequest.getHeaderNames().asIterator().forEachRemaining(name -> System.out.println(name + " = " + httpServletRequest.getHeader(name)));
			String servletPath = httpServletRequest.getServletPath();
			if(StringUtils.isBlank(servletPath)) {
				servletPath = httpServletRequest.getRequestURI();
			}
			System.out.println("method: " + httpServletRequest.getMethod());
			String urlRewrite = "/git/" + servletPath;
			request.getRequestDispatcher(urlRewrite).forward(request, response);
			return;
		}
		
		String servletPath = httpServletRequest.getServletPath();
		if(StringUtils.isBlank(servletPath)) {
			servletPath = httpServletRequest.getRequestURI();
		}
		
		// websocket 直接过
		String finalServletPath = servletPath;
		if(Arrays.stream(Resources.WS_ENDPOINTS).anyMatch(item -> finalServletPath.startsWith(item))) {
			chain.doFilter(request, response);
			return;
		}
		
		// Single Page Application 单页面应用的路由处理
		System.out.println("===============================================");
		System.out.println("===============================================");
		System.out.println("===============================================");
		System.out.println("servlet path = " + servletPath);
		httpServletRequest.getHeaderNames().asIterator().forEachRemaining(name -> System.out.println(name + " = " + httpServletRequest.getHeader(name)));
		
		System.out.println("url:" + servletPath);
		System.out.println("context path:" + httpServletRequest.getContextPath());
		System.out.println("pathInfo:" + httpServletRequest.getPathInfo());
		
		// 以下情况，不作处理，直接访问
		// 1. 访问首页
		// 2. 访问帮助文档中的图片
		if(servletPath.equals(WebSite.HOME_URL) || servletPath.startsWith("/raw/docs") ) {
			chain.doFilter(request, response);
			return;
		}
		
		String filenameExtension = UriUtils.extractFileExtension(servletPath);
		String referer = httpServletRequest.getHeader("referer");
		// 当按下浏览器的 F5，刷新 Single Page Application 的任一页面时，都跳转到首页
		// 如果是浏览器刷新，则 referer 的值必为 null
		boolean routerMatched = routerTemplates.stream().anyMatch(uriTemplate -> uriTemplate.matches(finalServletPath));
		boolean isStaticResource = isStaticResource(filenameExtension);
		// 能执行到这里，则一定是普通的浏览器请求，而不是 Fetch 请求
		// 而通过浏览器请求的，一律跳转到首页。
		if(!isStaticResource && 
				(routerMatched || needPrependServlet(servletPath))) {
			request.getRequestDispatcher(WebSite.HOME_URL).forward(request, response);
			return;
		}
		
		// 以下代码为转换资源文件的路径
		if(isStaticResource) {
			// 因为 /designer/assets 也需要返回静态资源，需要特殊处理
			if(servletPath.startsWith("/designer/assets")) {
				chain.doFilter(request, response);
				return;
			}
			
			if(referer == null) {
				// 因为访问 js.map 和 css.map 时，referer 的值为 null，但依然需要解析
				if(filenameExtension.equals("map")) {
					String sourceMapName = org.springframework.util.StringUtils.getFilename(servletPath);
					String sourceName = sourceMapName.substring(0, sourceMapName.length() - ".map".length());
					String sourceMapPath = sourceMap.get(sourceName) + ".map";
					request.getRequestDispatcher(sourceMapPath).forward(request, response);
					return;
				}
				
				chain.doFilter(request, response);
				return;
			}
			URI refererUri = URI.create(referer);
			String[] pathes = StringUtils.split(refererUri.getPath(), "/");
			String urlRewrite = Arrays.stream(pathes).reduce(servletPath, (result, item) -> {
				// 避免出现将 ab.js 截为 b.js 的情况
				String removed = "/" + item + "/";
				if(result.startsWith(removed)) {
					return result.substring(removed.length() - 1);
				}
				return result;
			});

			// cache for source map
			if(filenameExtension.equals("css") || filenameExtension.equals("js")) {
				String fileName = org.springframework.util.StringUtils.getFilename(servletPath);
				if(!sourceMap.containsKey(fileName)) {
					sourceMap.put(fileName, urlRewrite);
				}
			}
			
			request.getRequestDispatcher(urlRewrite).forward(request, response);
			return;
		}
		
		chain.doFilter(request, response);
	}

	/**
	 *  根据文件后缀名判断是否是静态资源文件
	 *  
	 * @param filenameExtension
	 * @return <code>true</code> 表示是静态资源，<code>false</code> 不是静态资源
	 */
	private boolean isStaticResource(String filenameExtension) {
		return StringUtils.isNotBlank(filenameExtension) && 
				ArrayUtils.contains(Resources.PUBLIC_FILE_EXTENSIONS, filenameExtension);
	}

	/**
	 * 判断是否需要在 servletPath 前追加 servlet 名
	 * 
	 * 为了设计用户友好，可读性高的 url，我们
	 * 将 /projects/{owner}/{project_name} 简写为 /{owner}/{project_name}；
	 * 将 /projects/new 简写为 /new
	 * 将 /users/{userName} 简写为 /{userName}。
	 * 
	 * 因此当请求发过来类似 /{owner}/{project_name} 或 /{userName} 的 url 后，
	 * 我们需要在前面补上 /projects 或 /users
	 * 
	 * @param servletPath
	 * @return 如果需要增补，则返回 <code>true</code>；否则返回 <code>false</code>。
	 */
	private boolean needPrependServlet(String servletPath) {
		if (StringUtils.equals("/", servletPath)) {
			return false;
		}
		if (!servletPath.startsWith("/")) {
			servletPath = "/" + servletPath;
		}
		// 精准判断 servlet path
		String[] pathSegment = servletPath.split("/");
		// 索引为 0 的值是空字符串，所以这里取第二个值
		String firstPath = pathSegment[1];
		for (String each : Resources.SERVLET_NAMES) {
			if (firstPath.equals(each)) {
				return false;
			}
		}
		return true;
	}

}
