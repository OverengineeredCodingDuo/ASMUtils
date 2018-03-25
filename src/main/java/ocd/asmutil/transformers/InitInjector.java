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

package ocd.asmutil.transformers;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.InsnInjector;
import ocd.asmutil.MethodNodeTransformer;
import ocd.asmutil.MethodTransformerException;
import ocd.asmutil.frame.FrameUtil;
import ocd.asmutil.frame.TrackingValue;
import ocd.asmutil.frame.TrackingValue.TrackingInterpreter;
import ocd.asmutil.matchers.MethodMatcher.MethodDescriptor;

public class InitInjector implements MethodNodeTransformer
{
	public static final InsnInjector CAPTURE_THIS = new ArgLoader(0);

	public static class ArgLoader implements InsnInjector
	{
		private final int index;

		public ArgLoader(final int index)
		{
			this.index = index;
		}

		@Override
		public void inject(
			final String className,
			final MethodNode methodNode,
			final AbstractInsnNode insn,
			final Frame<TrackingValue> frame,
			final Interpreter<TrackingValue> interpreter
		) throws AnalyzerException
		{
			final AbstractInsnNode loadInsn = new VarInsnNode(Opcodes.ALOAD, this.index);
			final AbstractInsnNode swapInsn = new InsnNode(Opcodes.SWAP);

			final InsnList insns = methodNode.instructions;

			insns.insertBefore(insn, loadInsn);
			insns.insertBefore(insn, swapInsn);

			frame.execute(loadInsn, interpreter);
			frame.execute(swapInsn, interpreter);
		}
	}

	private final MethodDescriptor md;
	private final InsnInjector[] injectors;

	public InitInjector(
		final String owner,
		final @Nullable String desc,
		final InsnInjector... injectors
	)
	{
		this.md = new MethodDescriptor(owner, "<init>", desc);
		this.injectors = injectors;
	}

	@Override
	public MethodNode transform(final String className, final MethodNode methodNode, final Logger logger) throws MethodTransformerException, AnalyzerException
	{
		final Interpreter<TrackingValue> interpreter = new TrackingInterpreter();
		final TrackingValue retType = interpreter.newValue(Type.getReturnType(methodNode.desc));

		final Frame<TrackingValue> frame = FrameUtil.getFrame(null, className, methodNode, retType, interpreter);

		final InsnList insns = methodNode.instructions;

		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext())
		{
			FrameUtil.execute(insn, frame, className, methodNode, retType, interpreter);

			if (!this.md.matches(insn))
				continue;

			final AbstractInsnNode next = insn.getNext();

			final AbstractInsnNode dupInsn = new InsnNode(Opcodes.DUP);

			insns.insertBefore(next, dupInsn);

			if (frame.getStackSize() == 0 || !frame.getStack(frame.getStackSize() - 1).getType().getInternalName().equals(this.md.owner))
				throw new MethodTransformerException("Missing instance of new object on operand stack: " + this.md.owner, methodNode, next);

			frame.execute(dupInsn, interpreter);

			for (final InsnInjector injector : this.injectors)
				injector.inject(className, methodNode, next, frame, interpreter);

			insn = next.getPrevious();
		}

		return methodNode;
	}
}
