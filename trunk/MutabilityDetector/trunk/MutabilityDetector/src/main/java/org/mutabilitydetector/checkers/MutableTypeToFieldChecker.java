/* 
 * Mutability Detector
 *
 * Copyright 2009 Graham Allan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.mutabilitydetector.checkers;

import static org.mutabilitydetector.IAnalysisSession.IsImmutable.IMMUTABLE;
import static org.mutabilitydetector.locations.Dotted.dotted;

import org.mutabilitydetector.IAnalysisSession;
import org.mutabilitydetector.IAnalysisSession.IsImmutable;
import org.mutabilitydetector.MutabilityReason;
import org.mutabilitydetector.checkers.info.TypeStructureInformation;
import org.mutabilitydetector.locations.Dotted;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;


public class MutableTypeToFieldChecker extends AbstractMutabilityChecker {

	private final IAnalysisSession analysisSession;
	private final TypeStructureInformation typeStructureInformation;

	public MutableTypeToFieldChecker(IAnalysisSession analysisSession, TypeStructureInformation typeStructureInformation) {
		this.analysisSession = analysisSession;
		this.typeStructureInformation = typeStructureInformation;
	}


	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new AssignMutableTypeToFieldChecker(ownerClass, access, name, desc, signature, exceptions);
	}

	class AssignMutableTypeToFieldChecker extends FieldAssignmentVisitor {


		public AssignMutableTypeToFieldChecker(String owner, int access, String name, String desc, String signature,
				String[] exceptions) {
			super(owner, access, name, desc, signature, exceptions);
		}

		@Override
		protected void visitFieldAssignmentFrame(Frame assignmentFrame, FieldInsnNode fieldInsnNode, BasicValue stackValue) {
			if (isInvalidStackValue(stackValue)) {
				return;
			}
			checkIfClassIsMutable(fieldInsnNode.name, stackValue.getType());
		}

		private void checkIfClassIsMutable(String name, Type type) {
			int sort = type.getSort();
			switch(sort) {
			case Type.OBJECT:
				String dottedClassName = dottedClassName(type);
				IsImmutable isImmutable = analysisSession.resultFor(dottedClassName).isImmutable;
				
				boolean isConcreteType = isConcreteType(dotted(dottedClassName));
				if (!isImmutable.equals(IMMUTABLE) && isConcreteType) {
					addResult("Field [" + name + "] can have a mutable type (" + dottedClassName + ") "
							+ "assigned to it.", null, MutabilityReason.MUTABLE_TYPE_TO_FIELD);
				}
				break;
			case Type.ARRAY:
				addResult("Field [" + name + "] can have a mutable type (a primitive array) "
						+ "assigned to it.", null, MutabilityReason.MUTABLE_TYPE_TO_FIELD);
				break;
			default:
				return;
			}
		}

		private boolean isConcreteType(Dotted className) {
			return !(typeStructureInformation.isTypeAbstract(className)
					|| typeStructureInformation.isTypeInterface(className));
		}

	}
}
