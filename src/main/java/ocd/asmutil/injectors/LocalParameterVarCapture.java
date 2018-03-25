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

import javax.annotation.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.MethodTransformerException;
import ocd.asmutil.SlicedInsnInjector;
import ocd.asmutil.frame.FrameUtil;
import ocd.asmutil.frame.FrameUtil.DynamicFrame;
import ocd.asmutil.frame.TrackingValue;
import ocd.asmutil.frame.TrackingValue.TrackingInterpreter;
import ocd.asmutil.matchers.MethodMatcher;
import ocd.asmutil.matchers.MethodMatcher.MethodDescriptor;

public class LocalParameterVarCapture implements SlicedInsnInjector
{
	private final int index;
	private final MethodMatcher.MethodDescriptor md;

	public LocalParameterVarCapture(final int index, final MethodDescriptor md)
	{
		this.index = index;
		this.md = md;
	}

	public LocalParameterVarCapture(final int index, final @Nullable String owner, final String name, final @Nullable String desc)
	{
		this(index, new MethodDescriptor(owner, name, desc));
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
		final VarInsnNode var = getVar(this.index, this.md, className, methodNode, sliceStart, sliceEnd, frameStart);

		final VarInsnNode varInsn = new VarInsnNode(var.getOpcode(), var.var);

		methodNode.instructions.insertBefore(sliceEnd, varInsn);

		frameEnd.execute(varInsn, interpreter);
	}

	public static VarInsnNode getVar(
		final int index,
		final MethodDescriptor md,
		final String className,
		final MethodNode methodNode,
		final AbstractInsnNode sliceStart,
		final AbstractInsnNode sliceEnd,
		final Frame<TrackingValue> frameStart
	) throws MethodTransformerException, AnalyzerException
	{
		final Interpreter<TrackingValue> interpreter = new TrackingInterpreter();
		final TrackingValue retType = interpreter.newValue(Type.getReturnType(methodNode.desc));

		final Frame<TrackingValue> frame = new DynamicFrame<>(frameStart, retType);

		VarInsnNode var = null;

		for (AbstractInsnNode insn = sliceStart; insn != null && insn != sliceEnd; insn = insn.getNext())
		{
			if (md.matches(insn))
			{
				final MethodInsnNode methodInsn = (MethodInsnNode) insn;

				int nArgs = Type.getArgumentTypes(methodInsn.desc).length;

				if (methodInsn.getOpcode() != Opcodes.INVOKESTATIC)
					++nArgs;

				final TrackingValue val = frame.getStack(frame.getStackSize() - nArgs + index);

				final AbstractInsnNode src = val.src;

				if (src == null || !(src instanceof VarInsnNode))
					throw new MethodTransformerException("Cannot track local variable for parameter " + index + ": " + md.toString(), methodNode, insn);

				final VarInsnNode varInsn = (VarInsnNode) src;

				if (var != null && var.var != varInsn.var)
					throw new MethodTransformerException("Found multiple invocations while trying to extract local variable: " + md.toString(), methodNode, insn);

				var = varInsn;
			}

			FrameUtil.execute(insn, frame, className, methodNode, retType, interpreter);
		}

		if (var == null)
			throw new MethodTransformerException("Found no invocation while trying to extract local variable: " + md.toString(), methodNode, sliceEnd);

		return var;
	}
}
