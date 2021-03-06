package com.blocklang.marketplace.apirepo.apiobject.webapi.change;

import com.blocklang.marketplace.apirepo.Change;
import com.blocklang.marketplace.apirepo.ChangeData;
import com.blocklang.marketplace.apirepo.ChangedObjectContext;
import com.blocklang.marketplace.apirepo.CodeGenerator;
import com.blocklang.marketplace.apirepo.apiobject.webapi.data.JsObjectData;

public class CreateObject extends Change{

	private JsObjectData data;
	
	@Override
	public void setData(ChangeData data) {
		this.data = (JsObjectData) data;
	}

	@Override
	public boolean apply(ChangedObjectContext context) {
		if(!validate(context)) {
			context.getLogger().error("Object.name {0} 已经被占用，请更换", data.getName());
			return false;
		}
		
		data.setId(context.getObjectId());
		data.setCode(context.nextObjectCode());
		CodeGenerator funcCodeGen = new CodeGenerator(null);
		data.getFunctions().forEach(func -> {
			func.setCode(funcCodeGen.next());
			
			CodeGenerator paramCodeGen = new CodeGenerator(null);
			func.getParameters().forEach(param -> param.setCode(paramCodeGen.next()));
		});
		
		context.addObject(data);
		return true;
	}

	private boolean validate(ChangedObjectContext context) {
		return !context.objectNameUsed(data.getName());
	}

}
