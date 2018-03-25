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
import org.objectweb.asm.tree.FieldInsnNode;

import ocd.asmutil.InjectionLocator.Simple;

public class FieldMatcher implements Simple
{
	private final int opcode;
	private final FieldMatcher.FieldDescriptor fd;

	public FieldMatcher(final int opcode, final FieldMatcher.FieldDescriptor fd)
	{
		this.opcode = opcode;
		this.fd = fd;
	}

	public FieldMatcher(final int opcode, final @Nullable String owner, final String name, final @Nullable String desc)
	{
		this(opcode, new FieldMatcher.FieldDescriptor(owner, name, desc));
	}

	public FieldMatcher(final int opcode, final String name)
	{
		this(opcode, null, name, null);
	}

	@Override
	public boolean test(final AbstractInsnNode insn)
	{
		if (this.opcode != insn.getOpcode())
			return false;

		return this.fd.matches(insn);
	}

	public static class FieldDescriptor
	{
		public final @Nullable String owner;
		public final String name;
		public final @Nullable String desc;

		public FieldDescriptor(final @Nullable String owner, final String name, final @Nullable String desc)
		{
			this.owner = owner == null ? null : owner.replace('.', '/');
			this.name = name;
			this.desc = desc;
		}

		public FieldDescriptor(final String name, @Nullable final String desc)
		{
			this(null, name, desc);
		}

		public FieldDescriptor(final String name)
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

		public boolean matches(final FieldInsnNode field)
		{
			return this.matches(field.owner, field.name, field.desc);
		}

		public boolean matches(final AbstractInsnNode insn)
		{
			if (!(insn instanceof FieldInsnNode))
				return false;

			return this.matches((FieldInsnNode) insn);
		}
	}
}
