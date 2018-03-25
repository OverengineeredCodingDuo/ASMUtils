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

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

class MethodSignature
{
	public final String name;
	public final @Nullable String desc;

	public MethodSignature(final String name, @Nullable final String desc)
	{
		this.name = name;
		this.desc = desc;
	}

	public MethodSignature(final String owner, final String name, @Nullable final String desc, final boolean obfuscated)
	{
		this(obfuscated ? FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc) : name, desc);
	}

	public @Nullable String owner()
	{
		return null;
	}

	public static class MethodDescriptor extends MethodSignature
	{
		public final String owner;

		public MethodDescriptor(final String owner, final String name, @Nullable final String desc, final boolean obfuscated)
		{
			super(owner, name, desc, obfuscated);
			this.owner = owner;
		}

		@Override
		public String owner()
		{
			return this.owner;
		}
	}
}
