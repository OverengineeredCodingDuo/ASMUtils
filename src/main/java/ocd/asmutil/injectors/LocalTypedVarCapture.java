/*
 * MIT License
 *
 * Copyright (c) 2017-2018 OverengineeredCodingDuo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ocd.asmutil.injectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.InsnInjector;
import ocd.asmutil.MethodTransformerException;
import ocd.asmutil.frame.TrackingValue;

public class LocalTypedVarCapture implements InsnInjector
{
	private final String desc;
	private final int opcode;

	public LocalTypedVarCapture(final Type type)
	{
		this.desc = type.getDescriptor();
		this.opcode = type.getOpcode(Opcodes.ILOAD);
	}

	public LocalTypedVarCapture(final String className)
	{
		this(Type.getObjectType(className));
	}

	@Override
	public void inject(
		final String className,
		final MethodNode methodNode,
		final AbstractInsnNode insn,
		final Frame<TrackingValue> frame,
		final Interpreter<TrackingValue> interpreter
	) throws MethodTransformerException, AnalyzerException
	{
		int var = -1;

		for (int i = 0; i < frame.getLocals(); ++i)
		{
			final Type type = frame.getLocal(i).getType();

			if (type == null || !this.desc.equals(type.getDescriptor()))
				continue;

			if (var != -1)
				throw new MethodTransformerException("Found multiple locals with requested type: " + this.desc, methodNode, insn);

			var = i;
		}

		if (var == -1)
			throw new MethodTransformerException("No local with requested type: " + this.desc, methodNode, insn);

		final AbstractInsnNode varInsn = new VarInsnNode(this.opcode, var);

		methodNode.instructions.insertBefore(insn, varInsn);

		frame.execute(varInsn, interpreter);
	}

	@Override
	public void inject(
		final String className,
		final MethodNode methodNode,
		final AbstractInsnNode sliceStart,
		final AbstractInsnNode sliceEnd,
		final Frame<TrackingValue> frameStart,
		final Frame<TrackingValue> frameEnd,
		final Interpreter<TrackingValue> interpreter
	) throws MethodTransformerException, AnalyzerException
	{
		final int var = getVar(this.opcode, this.desc, methodNode, sliceStart, sliceEnd, frameEnd);

		final VarInsnNode varInsn = new VarInsnNode(this.opcode, var);

		methodNode.instructions.insertBefore(sliceEnd, varInsn);

		frameEnd.execute(varInsn, interpreter);
	}

	public static int getVar(
		final int opcode,
		final String desc,
		final MethodNode methodNode,
		final AbstractInsnNode sliceStart,
		final AbstractInsnNode sliceEnd,
		final Frame<TrackingValue> frame
	) throws MethodTransformerException
	{
		int var = -1;

		for (AbstractInsnNode insn = sliceStart; insn != null && insn != sliceEnd; insn = insn.getNext())
		{
			if (!(insn instanceof VarInsnNode))
				continue;

			final VarInsnNode varInsn = (VarInsnNode) insn;

			if (varInsn.getOpcode() != opcode)
				continue;

			final Type type = frame.getLocal(varInsn.var).getType();

			if (type == null || !desc.equals(type.getDescriptor()))
				continue;

			if (var != -1)
				throw new MethodTransformerException("Found multiple locals with requested type: " + desc, methodNode, insn);

			var = varInsn.var;
		}

		if (var == -1)
			throw new MethodTransformerException("No local with requested type: " + desc, methodNode, sliceEnd);

		return var;
	}
}
