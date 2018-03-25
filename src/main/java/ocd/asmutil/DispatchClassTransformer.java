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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class DispatchClassTransformer implements net.minecraft.launchwrapper.IClassTransformer
{
	private final Logger logger;
	private final boolean verify;

	private final Map<String, List<IClassTransformer>> transformers = new HashMap<>();

	public DispatchClassTransformer(final Logger logger, final boolean verify)
	{
		this.logger = logger;
		this.verify = verify;
	}

	public DispatchClassTransformer addTransformer(
		final String name,
		final IClassTransformer... transformers
	)
	{
		final List<IClassTransformer> transformerList = this.transformers.computeIfAbsent(name, k -> new ArrayList<>());
		transformerList.addAll(Arrays.asList(transformers));

		return this;
	}

	public DispatchClassTransformer addTransformer(
		final IClassTransformer.Named... transformers
	)
	{
		for (final IClassTransformer.Named transformer : transformers)
			this.addTransformer(transformer.getName(), transformer);

		return this;
	}

	@Override
	public byte[] transform(final String name, final String transformedName, final byte[] basicClass)
	{
		final List<IClassTransformer> transformers = this.transformers.get(name);

		if (transformers == null)
			return basicClass;

		final ClassReader cr = new ClassReader(basicClass);
		final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

		ClassVisitor cv = cw;

		for (int i = transformers.size() - 1; i >= 0; --i)
			cv = transformers.get(i).createClassVisitor(this.logger, this.verify, Opcodes.ASM5, cv);

		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		return cw.toByteArray();
	}
}
