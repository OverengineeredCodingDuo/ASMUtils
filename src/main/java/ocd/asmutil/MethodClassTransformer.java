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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;

public class MethodClassTransformer implements IClassTransformer
{
	private final String name;

	public final MethodTransformer transformer;

	public MethodClassTransformer addTransformer(
		final String name,
		final @Nullable String desc,
		final boolean obfuscated,
		final MethodNodeTransformer... transformers
	)
	{
		this.transformer.addTransformer(name, desc, obfuscated, transformers);

		return this;
	}

	public MethodClassTransformer(final String name)
	{
		this(name, null, true);
	}

	public MethodClassTransformer(final String name, final Logger logger, final boolean verify)
	{
		this.name = name;

		this.transformer = new MethodTransformer(name.replace('.', '/'), logger, verify);
	}

	@Override
	public byte[] transform(final String name, final String transformedName, final byte[] basicClass)
	{
		if (!this.name.equals(transformedName))
			return basicClass;

		final ClassReader cr = new ClassReader(basicClass);
		final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

		cr.accept(this.transformer.createCV(Opcodes.ASM5, cw), ClassReader.EXPAND_FRAMES);
		return cw.toByteArray();
	}
}
