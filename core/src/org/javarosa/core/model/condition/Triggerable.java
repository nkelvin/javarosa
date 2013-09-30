/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.model.condition;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public abstract class Triggerable implements Externalizable {	
	public IConditionExpr expr;
	public Vector<TreeReference> targets;
	public TreeReference contextRef;  //generic ref used to turn triggers into absolute references
	public TreeReference originalContextRef;
	
	public Triggerable () {
		
	}
	
	public Triggerable (IConditionExpr expr, TreeReference contextRef) {
		this.expr = expr;
		this.contextRef = contextRef;
		this.originalContextRef = contextRef;
		this.targets = new Vector();
	}
	
	protected abstract Object eval (FormInstance instance, EvaluationContext ec);
	
	protected abstract void apply (TreeReference ref, Object result, FormInstance instance, FormDef f);
	
	public abstract boolean canCascade ();
	
	/**
	 * Not for re-implementation, dispatches all of the evaluation 
	 * @param instance
	 * @param evalContext
	 * @param f
	 */
	public final void apply (FormInstance instance, EvaluationContext parentContext, TreeReference context, FormDef f) {
		//The triggeringRoot is the highest level of actual data we can inquire about, but it _isn't_ necessarily the basis
		//for the actual expressions, so we need genericize that ref against the current context
		TreeReference ungenericised = originalContextRef.contextualize(context);
		EvaluationContext ec = new EvaluationContext(parentContext, ungenericised);

		Object result = eval(instance, ec);

		for (int i = 0; i < targets.size(); i++) {
			TreeReference targetRef = ((TreeReference)targets.elementAt(i)).contextualize(ec.getContextRef());
			Vector v = ec.expandReference(targetRef);		
			for (int j = 0; j < v.size(); j++) {
				TreeReference affectedRef = (TreeReference)v.elementAt(j);
				apply(affectedRef, result, instance, f);
			}
		}		
	}
	
	public void addTarget (TreeReference target) {
		if (targets.indexOf(target) == -1)
			targets.addElement(target);
	}
	
	public Vector<TreeReference> getTargets () {
		return targets;
	}
	
	public Vector<TreeReference> getTriggers () {
		Vector<TreeReference> relTriggers = expr.getTriggers();
		Vector<TreeReference> absTriggers = new Vector<TreeReference>();
		for (int i = 0; i < relTriggers.size(); i++) {
			absTriggers.addElement(((TreeReference)relTriggers.elementAt(i)).anchor(originalContextRef));
		}
		return absTriggers;		
	}

	public boolean equals (Object o) {
		if (o instanceof Triggerable) {
			Triggerable t = (Triggerable)o;
			if (this == t)
				return true;
			
			if (this.expr.equals(t.expr)) {
				//check triggers
				Vector<TreeReference> Atriggers = this.getTriggers();
				Vector<TreeReference> Btriggers = t.getTriggers();
				
				//order and quantity don't matter; all that matters is every trigger in A exists in B and vice versa
				for (int k = 0; k < 2; k++) {
					Vector<TreeReference> v1 = (k == 0 ? Atriggers : Btriggers);
					Vector<TreeReference> v2 = (k == 0 ? Btriggers : Atriggers);
				
					for (int i = 0; i < v1.size(); i++) {
						if (v2.indexOf(v1.elementAt(i)) == -1) {
							return false;
						}
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}			
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		expr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
		contextRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
		originalContextRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
		targets = (Vector<TreeReference>)ExtUtil.read(in, new ExtWrapList(TreeReference.class), pf);
	}
	
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, new ExtWrapTagged(expr));
		ExtUtil.write(out, contextRef);
		ExtUtil.write(out, originalContextRef);
		ExtUtil.write(out, new ExtWrapList(targets));
	}	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < targets.size(); i++) {
			sb.append(((TreeReference)targets.elementAt(i)).toString());
			if (i < targets.size() - 1)
				sb.append(",");
		}
		return "trig[expr:" + expr.toString() + ";targets[" + sb.toString() + "]]";
	}
}