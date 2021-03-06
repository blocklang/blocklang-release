import messageBundle from '../../../nls/main';
import * as c from '@blocklang/bootstrap-classes';
import * as css from './ListMyComponentRepo.m.css';
import ThemedMixin, { theme } from '@dojo/framework/core/mixins/Themed';
import I18nMixin from '@dojo/framework/core/mixins/I18n';
import WidgetBase from '@dojo/framework/core/WidgetBase';
import { v, w } from '@dojo/framework/core/vdom';
import Link from '@dojo/framework/routing/Link';
import Spinner from '../../../widgets/spinner';
import FontAwesomeIcon from '@blocklang/dojo-fontawesome/FontAwesomeIcon';
import { WithTarget, ComponentRepoPublishTask, ComponentRepoInfo, WsMessage } from '../../../interfaces';
import { ValidateStatus, PublishType, ReleaseResult } from '../../../constant';
import { UrlPayload } from '../../../processes/interfaces';
import Moment from '../../../widgets/moment';
import Exception from '../../error/Exception';
import { Client, IFrame, IStompSocket } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import {
	getRepoCategoryName,
	getProgramingLanguageName,
	getProgramingLanguageColor,
	getRepoTypeName,
} from '../../../util';
import { IconPrefix, IconName } from '@fortawesome/fontawesome-svg-core';

export interface ListMyComponentRepoProperties {
	loggedUsername: string;
	componentRepoUrl: string;
	publishTasks: ComponentRepoPublishTask[];
	componentRepoInfos: ComponentRepoInfo[];

	marketplacePageStatusCode: number;

	// validation
	repoUrlValidateStatus?: ValidateStatus;
	repoUrlErrorMessage?: string;
	repoUrlValidMessage?: string;

	// event
	onComponentRepoUrlInput: (opts: UrlPayload) => void;
	onPublishComponentRepo: (opts: object) => void;
	reloadUserComponentRepos: (opts: object) => void;
}

@theme(css)
export default class ListMyComponentRepo extends ThemedMixin(I18nMixin(WidgetBase))<ListMyComponentRepoProperties> {
	private _localizedMessages = this.localizeBundle(messageBundle);

	private _wsClient: Client;

	constructor() {
		super();

		this._wsClient = new Client({});

		this._wsClient.webSocketFactory = function () {
			return new SockJS('/release-console') as IStompSocket;
		};

		this._wsClient.onStompError = function (frame: IFrame) {
			console.log('Broker reported error: ' + frame.headers['message']);
			console.log('Additional details: ' + frame.body);
		};
	}

	protected onDetach() {
		if (this._wsClient.active) {
			this._wsClient.deactivate();
		}
	}

	protected render() {
		if (!this._isAuthenticated()) {
			return w(Exception, { type: '403' });
		}

		return v('div', { classes: [css.root, c.container, c.mt_5] }, [
			v('div', { classes: [c.row] }, [
				v('div', { classes: [c.col_3] }, [this._renderMenu()]),
				v('div', { classes: [c.col_9] }, [
					this._renderHeader(),
					this._renderPublishForm(),
					this._renderPublishTasksBlock(),
					this._renderCompomentReposBlock(),
				]),
			]),
		]);
	}

	private _isAuthenticated() {
		const { loggedUsername } = this.properties;
		return !!loggedUsername;
	}

	private _renderMenu() {
		const { messages } = this._localizedMessages;

		return v('ul', { classes: [c.list_group] }, [
			v('li', { classes: [c.list_group_item] }, [
				w(Link, { to: 'settings-profile' }, [`${messages.userSettingMenuProfile}`]),
			]),
			v('li', { classes: [c.list_group_item, css.active] }, [`${messages.userSettingMenuMarketplace}`]),
		]);
	}

	private _renderHeader() {
		const { messages } = this._localizedMessages;
		return v('div', [v('h4', [`${messages.userSettingMenuMarketplace}`]), v('hr')]);
	}

	// 发布组件 form 表单
	// 点击按钮，或按下回车键提交
	// 校验 url 是否有效的 git 仓库地址
	// 1. 格式有效,支持 https 协议
	// 2. 属于公开仓库，能够访问
	private _renderPublishForm() {
		const { repoUrlValidateStatus, repoUrlErrorMessage, repoUrlValidMessage } = this.properties;
		const inputClasses = [c.form_control];

		let repoUrlMessageVDom = null;
		if (repoUrlValidateStatus === ValidateStatus.INVALID) {
			repoUrlMessageVDom = v('div', { classes: [c.invalid_tooltip] }, [`${repoUrlErrorMessage}`]);
			inputClasses.push(c.is_invalid);
		} else if (repoUrlValidateStatus === ValidateStatus.VALID) {
			if (repoUrlValidMessage) {
				repoUrlMessageVDom = v('div', { classes: [c.valid_tooltip] }, [`${repoUrlValidMessage}`]);
				inputClasses.push(c.is_valid);
			}
		}

		return v('form', { classes: [c.needs_validation, c.mb_4], novalidate: 'novalidate' }, [
			v('div', { classes: [c.mb_3] }, [
				v('div', { classes: [c.input_group] }, [
					v('input', {
						type: 'text',
						classes: inputClasses,
						placeholder: 'HTTPS 协议的 Git 仓库地址，如 https://github.com/blocklang/widgets-bootstrap.git',
						'aria-label': 'git 仓库地址',
						'aria-describedby': 'btn-addon',
						oninput: this._onComponentRepoUrlInput,
						onkeydown: (event: KeyboardEvent<HTMLInputElement>) => {
							if (event.code === 'Enter' /* 敲回车 */) {
								event.preventDefault();
								this._publishComponentRepo();
							}
						},
					}),
					v('div', { classes: [c.input_group_text] }, [
						v(
							'button',
							{
								classes: [c.btn, c.btn_outline_primary],
								type: 'button',
								id: 'btn-addon',
								onclick: this._publishComponentRepo,
							},
							['发布']
						),
					]),
					repoUrlMessageVDom,
				]),
				v('small', { classes: [c.form_text, c.text_muted] }, ['填写组件仓库的 HTTPS 协议克隆地址']),
			]),
		]);
	}

	// TODO: 需增加：实时显示新增内容
	private _renderPublishTasksBlock() {
		const { publishTasks } = this.properties;

		if (!publishTasks) {
			return w(Spinner, {});
		}

		if (publishTasks.length > 0) {
			if (!this._wsClient.active) {
				this._wsClient.onConnect = (frame: IFrame) => {
					publishTasks.forEach((item) => {
						this._wsClient.subscribe(`/topic/publish/${item.id}`, (message) => {
							const body = JSON.parse(message.body);
							const {
								headers: { event, releaseResult },
							} = body as WsMessage;
							if (event === 'finish') {
								item.publishResult = releaseResult!;
								this._onReloadComponentRepos();
								this.invalidate();
							}
						});
					});
				};
				this._wsClient.activate();
			}

			const filterPublishTask = publishTasks.filter((task) => task.publishResult === ReleaseResult.Started);
			if (filterPublishTask.length > 0) {
				return v('div', { classes: [c.mb_4] }, [
					v('h6', { classes: c.fw_normal }, ['正在发布']),
					v(
						'ul',
						{ classes: [c.list_group, c.mt_2] },
						filterPublishTask.map((task) => this._renderTask(task))
					),
				]);
			}
		}
	}

	private _onReloadComponentRepos() {
		this.properties.reloadUserComponentRepos({});
	}

	private _renderTask(task: ComponentRepoPublishTask) {
		return v('li', { classes: [c.list_group_item, c.d_flex] }, [
			v(
				'div',
				{
					classes: [c.spinner_border, c.spinner_border_sm, c.text_warning, c.me_2, c.mt_1],
					role: 'status',
				},
				[v('span', { classes: [c.visually_hidden] }, ['发布中……'])]
			),
			v('div', { classes: [c.flex_grow_1] }, [
				v('div', {}, [
					v('a', { href: `${task.gitUrl}`, classes: [c.fw_bold], target: '_blank' }, [`${task.gitUrl}`]),
					w(
						Link,
						{
							to: 'view-component-repo-publish-task',
							params: { taskId: `${task.id}` },
							classes: [c.float_end],
						},
						['发布日志']
					),
				]),
				v('div', { classes: [c.text_muted, c.mt_2] }, [
					v('span', { classes: [c.me_2], title: '任务编号' }, [`#${task.seq}`]),
					v('span', {}, [
						w(FontAwesomeIcon, { icon: 'clock' }),
						task.publishType === PublishType.New
							? ' 首次发布 · '
							: ` 升级 ${task.fromVersion} -> ${task.toVersion} · `,
						w(Moment, { datetime: task.startTime }),
					]),
				]),
			]),
		]);
	}

	private _publishComponentRepo() {
		this.properties.onPublishComponentRepo({});
	}

	private _onComponentRepoUrlInput({ target: { value: url } }: WithTarget) {
		this.properties.onComponentRepoUrlInput({ url });
	}

	private _renderCompomentReposBlock() {
		const { componentRepoInfos } = this.properties;

		if (!componentRepoInfos) {
			return w(Spinner, {});
		}

		if (componentRepoInfos.length === 0) {
			return this._renderEmptyComponentRepo();
		}

		return this._renderComponentRepos();
	}

	private _renderEmptyComponentRepo() {
		const { messages } = this._localizedMessages;

		return v(
			'div',
			{ key: 'empty', classes: [c.bg_light, c.mx_auto, c.text_center, c.mt_5], styles: { maxWidth: '544px' } },
			[
				w(FontAwesomeIcon, { icon: 'puzzle-piece', size: '2x', classes: [c.text_muted] }),
				v('h3', { classes: [c.mt_3] }, [`${messages.noComponentTitle}`]),
				v('p', [
					v('ol', { classes: [c.text_start] }, [
						v('li', [`${messages.noComponentTipLine1}`]),
						v('li', [`${messages.noComponentTipLine2}`]),
						v('li', [`${messages.noComponentTipLine3}`]),
					]),
				]),
			]
		);
	}

	private _renderComponentRepos() {
		const { componentRepoInfos } = this.properties;

		return v('div', { key: 'repos' }, [
			v('h6', { classes: [c.fw_normal] }, ['已发布']),
			v(
				'ul',
				{ classes: [c.list_group, c.mt_2] },
				componentRepoInfos.map((repo) => {
					const { componentRepo, componentRepoVersion, apiRepo } = repo;

					return v('li', { classes: [c.list_group_item] }, [
						v('div', {}, [
							v('div', {}, [
								v('span', { classes: [c.fw_bold, c.me_3] }, [`${componentRepoVersion.name}`]),
								componentRepo.std
									? v(
											'span',
											{
												classes: [c.badge, c.bg_info, c.ms_1],
												title: '项目的默认依赖',
											},
											['标准库']
									  )
									: undefined,
								v(
									'span',
									{
										classes: [c.badge, c.bg_info, c.ms_1],
										title: '与 BlockLang 设计器集成',
									},
									[`${getRepoTypeName(componentRepo.repoType)}`]
								),
							]),
							componentRepoVersion.description &&
								v('p', { itemprop: 'description', classes: [c.text_muted, c.mb_0] }, [
									`${componentRepoVersion.description}`,
								]),
							v('div', { classes: [c.my_2] }, [
								v('span', { classes: [c.border, c.rounded, c.px_1] }, [
									v('span', {}, ['API: ']),
									v(
										'a',
										{
											target: '_blank',
											href: `${apiRepo.gitRepoUrl}`,
											title: '跳转到 API 仓库',
											classes: [c.me_1],
										},
										[`${apiRepo.gitRepoOwner}/${apiRepo.gitRepoName}`]
									),
								]),
								' -> ',
								v('span', { classes: [c.border, c.rounded, c.px_1] }, [
									v('span', {}, [`${getRepoTypeName(componentRepo.repoType)}: `]),
									v(
										'a',
										{
											target: '_blank',
											href: `${componentRepo.gitRepoUrl}`,
											title: '跳转到组件仓库',
											classes: [c.me_1],
										},
										[`${componentRepo.gitRepoOwner}/${componentRepo.gitRepoName}`]
									),
								]),
							]),
							v('small', { classes: [c.text_muted] }, [
								v('span', { classes: [c.me_3] }, [
									w(FontAwesomeIcon, {
										icon: componentRepoVersion.icon.split(' ') as [IconPrefix, IconName],
										classes: [c.me_1],
									}),
									`${componentRepoVersion.title}`,
								]),
								v('span', { classes: [c.me_3] }, [
									v('span', {
										classes: [css.repoLanguageColor, c.me_1],
										styles: {
											backgroundColor: `${getProgramingLanguageColor(
												componentRepoVersion.language
											)}`,
										},
									}),
									v('span', { itemprop: 'programmingLanguage' }, [
										`${getProgramingLanguageName(componentRepoVersion.language)}`,
									]),
								]),
								v('span', { classes: [c.me_3] }, [`${getRepoCategoryName(componentRepo.category)}`]),
								v('span', { classes: [c.me_3], title: '使用次数' }, [
									w(FontAwesomeIcon, { icon: 'cube', classes: [c.me_1] }),
									'0',
								]),
								v('span', {}, [
									w(FontAwesomeIcon, { icon: 'clock', classes: [c.me_1] }),
									'最近发布 · ',
									w(Moment, { datetime: componentRepo.lastPublishTime }),
								]),
							]),
						]),
					]);
				})
			),
		]);
	}
}
