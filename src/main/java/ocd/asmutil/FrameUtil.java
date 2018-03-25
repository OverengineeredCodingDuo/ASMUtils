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

import java.util.List;
import javax.annotation.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public class FrameUtil
{
	static <V extends Value> Frame<V> getFrame(
		final @Nullable FrameNode frameNode,
		final String className,
		final MethodNode methodNode,
		final V retType,
		final Interpreter<V> interpreter
	) throws MethodTransformerException
	{
		if (frameNode == null)
		{
			final Type[] args = Type.getArgumentTypes(methodNode.desc);
			final boolean staticAcc = (methodNode.access & Opcodes.ACC_STATIC) != 0;

			final Frame<V> frame = new DynamicFrame<>(methodNode.maxLocals, 0);
			frame.setReturn(retType);

			if (!staticAcc)
				frame.setLocal(0, interpreter.newValue(Type.getObjectType(className)));

			for (int i = 0; i < args.length; ++i)
				frame.setLocal(staticAcc ? i : i + 1, interpreter.newValue(args[i]));

			return frame;
		}

		if (frameNode.type != Opcodes.F_NEW)
			throw new MethodTransformerException("Require expanded frames", methodNode, frameNode);

		final List<Object> locals = frameNode.local;
		final List<Object> stack = frameNode.stack;

		final Frame<V> frame = new DynamicFrame<>(methodNode.maxLocals, methodNode.maxStack);
		frame.setReturn(retType);

		for (int i = 0; i < locals.size(); ++i)
			frame.setLocal(i, getType(locals.get(i), interpreter));

		for (int i = 0; i < stack.size(); ++i)
			frame.push(getType(stack.get(i), interpreter));

		return frame;
	}

	public static <V extends Value> void execute(
		final AbstractInsnNode insn,
		final Frame<V> frame,
		final String className,
		final MethodNode methodNode,
		final V retType,
		final Interpreter<V> interpreter) throws MethodTransformerException, AnalyzerException
	{
		if (insn instanceof FrameNode)
		{
			frame.init(FrameUtil.getFrame((FrameNode) insn, className, methodNode, retType, interpreter));
			frame.setReturn(retType);
		}
		else if (!(insn instanceof LabelNode) && !(insn instanceof LineNumberNode))
			frame.execute(insn, interpreter);
	}

	@Nullable
	private static <V extends Value> V getType(final Object type, final Interpreter<V> interpreter)
	{
		if (type instanceof String)
			return interpreter.newValue(Type.getObjectType((String) type));
		else if (type instanceof Integer)
		{
			final int intType = (Integer) type;

			if (intType == Opcodes.INTEGER)
				return interpreter.newValue(Type.INT_TYPE);
			else if (intType == Opcodes.FLOAT)
				return interpreter.newValue(Type.FLOAT_TYPE);
			else if (intType == Opcodes.LONG)
				return interpreter.newValue(Type.LONG_TYPE);
			else if (intType == Opcodes.DOUBLE)
				return interpreter.newValue(Type.DOUBLE_TYPE);
			else if (intType == Opcodes.NULL)
				return null;
		}

		return interpreter.newValue(null);
	}

	private static <V extends Value> void copyFrame(final Frame<? extends V> srcFrame, final Frame<V> targetFrame, final V retVal)
	{
		targetFrame.setReturn(retVal);

		for (int i = 0; i < srcFrame.getLocals(); ++i)
			targetFrame.setLocal(i, srcFrame.getLocal(i));

		for (int i = 0; i < srcFrame.getStackSize(); ++i)
			targetFrame.push(srcFrame.getStack(i));
	}

	public static class DynamicFrame<V extends Value> extends Frame<V>
	{
		private Frame<V> frame;
		V reVal;

		public DynamicFrame(final int nLocals, final int nStack)
		{
			super(0, 0);

			this.frame = new Frame<>(nLocals, nStack);
		}

		public DynamicFrame(final Frame<? extends V> src, final V retVal)
		{
			this(src.getLocals(), src.getMaxStackSize());
			this.init(src);
			this.setReturn(retVal);
		}

		@Override
		public Frame<V> init(final Frame<? extends V> src)
		{
			this.frame = new Frame<>(Math.max(this.getLocals(), src.getLocals()), Math.max(this.getMaxStackSize(), src.getMaxStackSize()));
			copyFrame(src, this.frame, this.reVal);

			return this.frame;
		}

		@Override
		public void setReturn(final V v)
		{
			this.reVal = v;
			super.setReturn(v);
			this.frame.setReturn(v);
		}

		@Override
		public int getLocals()
		{
			return this.frame.getLocals();
		}

		@Override
		public int getMaxStackSize()
		{
			return this.frame.getMaxStackSize();
		}

		@Override
		public V getLocal(final int i) throws IndexOutOfBoundsException
		{
			return this.frame.getLocal(i);
		}

		@Override
		public void setLocal(final int i, final V value) throws IndexOutOfBoundsException
		{
			this.frame.setLocal(i, value);
		}

		@Override
		public int getStackSize()
		{
			return this.frame.getStackSize();
		}

		@Override
		public V getStack(final int i) throws IndexOutOfBoundsException
		{
			return this.frame.getStack(i);
		}

		@Override
		public void clearStack()
		{
			this.frame.clearStack();
		}

		@Override
		public V pop() throws IndexOutOfBoundsException
		{
			return this.frame.pop();
		}

		@Override
		public void push(final V value) throws IndexOutOfBoundsException
		{
			if (this.getStackSize() == this.getMaxStackSize())
			{
				final Frame<V> frame = this.frame;
				this.frame = new Frame<>(this.getLocals(), this.getMaxStackSize() + 1);
				copyFrame(frame, this.frame, this.reVal);
			}

			this.frame.push(value);
		}

		@Override
		public boolean merge(final Frame<? extends V> frame, final Interpreter<V> interpreter) throws AnalyzerException
		{
			return this.frame.merge(frame, interpreter);
		}

		@Override
		public boolean merge(final Frame<? extends V> frame, final boolean[] access)
		{
			return this.frame.merge(frame, access);
		}

		@Override
		public String toString()
		{
			return this.frame.toString();
		}
	}
}
