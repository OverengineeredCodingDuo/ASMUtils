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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import ocd.asmutil.MethodSignature.MethodDescriptor;

public class InvokeInjector implements InsnInjector
{
	private final MethodSignature md;
	private final boolean iface;
	private final int opcode;

	public InvokeInjector(
		final @Nullable String owner,
		final String name,
		final String desc,
		final boolean obfuscated,
		final boolean iface,
		final boolean isStatic
	)
	{
		this.md = owner == null ? new MethodSignature(name, desc) : new MethodDescriptor(owner, name, desc, obfuscated);
		this.iface = iface;

		if (isStatic)
			this.opcode = Opcodes.INVOKESTATIC;
		else if (iface)
			this.opcode = Opcodes.INVOKEINTERFACE;
		else
			this.opcode = Opcodes.INVOKEVIRTUAL;
	}

	public InvokeInjector(
		@Nullable final String owner,
		final String name,
		final String desc,
		final boolean obfuscated,
		final boolean iface
	)
	{
		this(owner, name, desc, obfuscated, iface, false);
	}

	public InvokeInjector(
		final String name,
		final String desc,
		final boolean obfuscated
	)
	{
		this(null, name, desc, obfuscated, false);
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
		@Nullable final String owner = this.md.owner();
		@Nullable final String desc = this.md.desc;

		if (desc == null)
			throw new IllegalStateException("Injection with null descriptor");

		final AbstractInsnNode invokeInsn = new MethodInsnNode(this.opcode, owner == null ? className : owner, this.md.name, desc, this.iface);

		methodNode.instructions.insertBefore(
			insn,
			invokeInsn
		);

		frame.execute(invokeInsn, interpreter);
	}
}
