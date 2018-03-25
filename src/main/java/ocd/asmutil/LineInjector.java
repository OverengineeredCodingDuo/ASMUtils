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

package ocd.asmutil;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.FrameUtil.DynamicFrame;
import ocd.asmutil.TrackingValue.TrackingInterpreter;

public class LineInjector implements MethodNodeTransformer
{
	private final LineProcessor processor;

	public static final SlicedInsnInjector REMOVE = (className, methodNode, sliceStart, sliceEnd, frameStart, frameEnd, interpreter) -> {
		InsnList insns = methodNode.instructions;
		InsnList removedInsns = new InsnList();

		AbstractInsnNode end = sliceStart.getPrevious();

		for (AbstractInsnNode insn = sliceEnd.getPrevious(); insn != end; )
		{
			AbstractInsnNode next = insn.getPrevious();

			insns.remove(insn);
			removedInsns.insert(insn);

			insn = next;
		}

		insns.insertBefore(sliceEnd, new InsnNode(Opcodes.NOP));

		frameEnd.init(frameStart);
	};

	public LineInjector(final LineProcessor processor)
	{
		this.processor = processor;
	}

	public LineInjector(
		final InjectionLocator lineIdentifier,
		final @Nullable InjectionLocator lineEndIdentifier,
		final int stackMod,
		final SlicedInsnInjector... injectors
	)
	{
		this(new LineProcessor(lineIdentifier, lineEndIdentifier, injectors, stackMod));
	}

	public LineInjector(final InjectionLocator lineIdentifier, final SlicedInsnInjector... injectors)
	{
		this(lineIdentifier, null, 0, injectors);
	}

	@Override
	public MethodNode transform(final String className, final MethodNode methodNode, final Logger logger) throws MethodTransformerException, AnalyzerException
	{
		final Interpreter<TrackingValue> interpreter = new TrackingInterpreter();

		final TrackingValue retType = interpreter.newValue(Type.getReturnType(methodNode.desc));

		final Frame<TrackingValue> frame = FrameUtil.getFrame(null, className, methodNode, retType, interpreter);

		FrameNode lastFrame = null;

		final InsnList insns = methodNode.instructions;

		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn == null ? insns.getFirst() : insn.getNext())
		{
			if (insn instanceof FrameNode)
				lastFrame = (FrameNode) insn;

			FrameUtil.execute(insn, frame, className, methodNode, retType, interpreter);

			if (!this.processor.lineIdentifier.test(methodNode, insn))
				continue;

			final InjectionLocator lineEndIdentifier = this.processor.lineEndIdentifier;

			final AbstractInsnNode lineInsn = insn;

			for (; insn != null; insn = insn.getNext())
			{
				if (insn != lineInsn)
					FrameUtil.execute(insn, frame, className, methodNode, retType, interpreter);

				if (lineEndIdentifier == null && frame.getStackSize() == 0 || lineEndIdentifier != null && lineEndIdentifier.test(methodNode, insn))
					break;
			}

			if (insn == null)
				throw new MethodTransformerException("Could not find end of line", methodNode, lineInsn);

			final int stackSize = frame.getStackSize() - this.processor.stackMod;

			final AbstractInsnNode end = insn.getNext();

			final Frame<TrackingValue> lineFrame = FrameUtil.getFrame(lastFrame, className, methodNode, retType, interpreter);

			final Frame<TrackingValue> frameStart = new DynamicFrame<>(frame, retType);
			AbstractInsnNode candidateStart = end;

			for (insn = lastFrame == null ? insns.getFirst() : lastFrame.getNext(); insn != end; insn = insn.getNext())
			{
				if (lineFrame.getStackSize() == stackSize)
				{
					candidateStart = insn;
					frameStart.init(lineFrame);
				}

				FrameUtil.execute(insn, lineFrame, className, methodNode, retType, interpreter);
			}

			for (final SlicedInsnInjector injector : this.processor.injectors)
				injector.inject(className, methodNode, candidateStart, end, frameStart, frame, interpreter);

			insn = end.getPrevious();
		}

		return methodNode;
	}

	public static class LineProcessor
	{
		public final InjectionLocator lineIdentifier;
		public final @Nullable InjectionLocator lineEndIdentifier;
		public final SlicedInsnInjector[] injectors;
		final int stackMod;

		private LineProcessor(
			final InjectionLocator lineIdentifier,
			final @Nullable InjectionLocator lineEndIdentifier,
			final SlicedInsnInjector[] injectors,
			final int stackMod)
		{
			this.lineIdentifier = lineIdentifier;
			this.lineEndIdentifier = lineEndIdentifier;
			this.injectors = injectors;
			this.stackMod = stackMod;
		}
	}
}
