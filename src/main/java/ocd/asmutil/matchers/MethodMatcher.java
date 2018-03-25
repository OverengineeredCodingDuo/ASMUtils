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

package ocd.asmutil.matchers;

import javax.annotation.Nullable;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ocd.asmutil.InjectionLocator.Simple;

public class MethodMatcher implements Simple
{
	private final MethodDescriptor md;

	public MethodMatcher(final MethodDescriptor md)
	{
		this.md = md;
	}

	public MethodMatcher(final @Nullable String owner, final String name, final @Nullable String desc)
	{
		this(new MethodDescriptor(owner, name, desc));
	}

	public MethodMatcher(final String name)
	{
		this(null, name, null);
	}

	@Override
	public boolean test(final AbstractInsnNode insn)
	{
		return this.md.matches(insn);
	}

	public static class MethodDescriptor
	{
		public final @Nullable String owner;
		public final String name;
		public final @Nullable String desc;

		public MethodDescriptor(final @Nullable String owner, final String name, final @Nullable String desc)
		{
			this.owner = owner == null ? null : owner.replace('.', '/');
			this.name = name;
			this.desc = desc;
		}

		public MethodDescriptor(final String name, @Nullable final String desc)
		{
			this(null, name, desc);
		}

		public MethodDescriptor(final String name)
		{
			this(name, null);
		}

		public boolean matches(final String owner, final String name, final String desc)
		{
			if (this.owner != null && !this.owner.equals(owner))
				return false;

			if (this.desc != null && !this.desc.equals(desc))
				return false;

			return this.name.equals(name);
		}

		public boolean matches(final MethodInsnNode method)
		{
			return this.matches(method.owner, method.name, method.desc);
		}

		public boolean matches(final AbstractInsnNode insn)
		{
			if (!(insn instanceof MethodInsnNode))
				return false;

			return this.matches((MethodInsnNode) insn);
		}
	}
}
