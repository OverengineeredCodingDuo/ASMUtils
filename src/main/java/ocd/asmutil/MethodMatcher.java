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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ocd.asmutil.MethodSignature.MethodDescriptor;

public class MethodMatcher implements InjectionLocator.Simple
{
	private final MethodDescriptor md;

	public MethodMatcher(final MethodDescriptor md)
	{
		this.md = md;
	}

	public MethodMatcher(final String owner, final String name, final @Nullable String desc, final boolean obfuscated)
	{
		this(new MethodDescriptor(owner, name, desc, obfuscated));
	}

	@Override
	public boolean test(final AbstractInsnNode insn)
	{
		if (!(insn instanceof MethodInsnNode))
			return false;

		final MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

		if (!this.md.owner.equals(methodInsnNode.owner))
			return false;

		if (this.md.desc != null && !this.md.desc.equals(methodInsnNode.desc))
			return false;

		return this.md.name.equals(methodInsnNode.name);
	}
}
