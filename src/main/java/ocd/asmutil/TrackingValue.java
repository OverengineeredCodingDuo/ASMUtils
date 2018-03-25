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
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;

public class TrackingValue extends BasicValue
{
	public static final TrackingValue UNINITIALIZED_VALUE = new TrackingValue((Type) null, null);

	public static final TrackingValue INT_VALUE = new TrackingValue(Type.INT_TYPE, null);

	public static final TrackingValue FLOAT_VALUE = new TrackingValue(Type.FLOAT_TYPE, null);

	public static final TrackingValue LONG_VALUE = new TrackingValue(Type.LONG_TYPE, null);

	public static final TrackingValue DOUBLE_VALUE = new TrackingValue(Type.DOUBLE_TYPE, null);

	public final @Nullable AbstractInsnNode src;

	public TrackingValue(final @Nullable Type type, final @Nullable AbstractInsnNode src)
	{
		super(type);

		this.src = src;
	}

	@Override
	public boolean equals(final Object value)
	{
		if (value == this)
			return true;

		if (value instanceof TrackingValue)
		{
			final TrackingValue val = (TrackingValue) value;

			return super.equals(val) && this.src == val.src;
		}

		return false;
	}

	public static class TypedInterpreter extends BasicInterpreter
	{
		@Override
		public BasicValue newValue(final Type type)
		{
			if (type != null && type.getSort() == Type.OBJECT)
				return new BasicValue(type);

			return super.newValue(type);
		}
	}

	public static class TrackingInterpreter extends Interpreter<TrackingValue>
	{
		private final Interpreter<BasicValue> interpreter = new TypedInterpreter();

		public TrackingInterpreter()
		{
			super(Opcodes.ASM5);
		}

		public TrackingInterpreter(final int api)
		{
			super(api);
		}

		@Override
		public @Nullable TrackingValue newValue(final Type type)
		{
			if (type == null)
				return TrackingValue.UNINITIALIZED_VALUE;

			switch (type.getSort())
			{
			case Type.VOID:
				return null;
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
				return TrackingValue.INT_VALUE;
			case Type.FLOAT:
				return TrackingValue.FLOAT_VALUE;
			case Type.LONG:
				return TrackingValue.LONG_VALUE;
			case Type.DOUBLE:
				return TrackingValue.DOUBLE_VALUE;
			case Type.ARRAY:
			case Type.OBJECT:
				return new TrackingValue(type, null);
			default:
				throw new Error("Internal error");
			}
		}

		private static @Nullable TrackingValue newTrackingValue(final @Nullable BasicValue val, final @Nullable AbstractInsnNode insn)
		{
			return val == null ? null : new TrackingValue(val.getType(), insn);
		}

		@Override
		public TrackingValue newOperation(final AbstractInsnNode insn) throws AnalyzerException
		{
			return newTrackingValue(this.interpreter.newOperation(insn), insn);
		}

		@Override
		public TrackingValue copyOperation(final AbstractInsnNode insn, final TrackingValue value) throws AnalyzerException
		{
			return value;
		}

		@Override
		public TrackingValue unaryOperation(final AbstractInsnNode insn, final TrackingValue value) throws AnalyzerException
		{
			return newTrackingValue(this.interpreter.unaryOperation(insn, value), insn);
		}

		@Override
		public TrackingValue binaryOperation(final AbstractInsnNode insn, final TrackingValue value1, final TrackingValue value2) throws AnalyzerException
		{
			return newTrackingValue(this.interpreter.binaryOperation(insn, value1, value2), insn);
		}

		@Override
		public TrackingValue ternaryOperation(final AbstractInsnNode insn, final TrackingValue value1, final TrackingValue value2, final TrackingValue value3) throws AnalyzerException
		{
			return newTrackingValue(this.interpreter.ternaryOperation(insn, value1, value2, value3), insn);
		}

		@Override
		public TrackingValue naryOperation(final AbstractInsnNode insn, final List<? extends TrackingValue> values) throws AnalyzerException
		{
			return newTrackingValue(this.interpreter.naryOperation(insn, values), insn);
		}

		@Override
		public void returnOperation(final AbstractInsnNode insn, final TrackingValue value, final TrackingValue expected) throws AnalyzerException
		{
			this.interpreter.returnOperation(insn, value, expected);
		}

		@Override
		public TrackingValue merge(final TrackingValue trackingValue, final TrackingValue w)
		{
			if (trackingValue.equals(w))
				return w;

			return newTrackingValue(this.interpreter.merge(trackingValue, w), null);
		}
	}
}
