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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.frame.TrackingValue;

public interface InsnInjector extends SlicedInsnInjector
{
	void inject(
		String className,
		MethodNode methodNode,
		AbstractInsnNode insn,
		final Frame<TrackingValue> frame,
		Interpreter<TrackingValue> interpreter
	) throws MethodTransformerException, AnalyzerException;

	@Override
	default void inject(
		final String className,
		final MethodNode methodNode,
		final AbstractInsnNode sliceStart,
		final AbstractInsnNode sliceEnd,
		final Frame<TrackingValue> frameStart,
		final Frame<TrackingValue> frameEnd,
		final Interpreter<TrackingValue> interpreter
	) throws MethodTransformerException, AnalyzerException
	{
		this.inject(className, methodNode, sliceEnd, frameEnd, interpreter);
	}
}
