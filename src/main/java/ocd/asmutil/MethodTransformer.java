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
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ocd.asmutil.MethodSignature.MethodDescriptor;

public class MethodTransformer
{
	private final String internalName;
	private final Logger logger;
	private final boolean verify;

	private final Multimap<String, Pair<MethodDescriptor, MethodNodeTransformer[]>> transformers = ArrayListMultimap.create();

	public MethodTransformer(final String internalName, final Logger logger, final boolean verify)
	{
		this.internalName = internalName;
		this.logger = logger;
		this.verify = verify;
	}

	public MethodTransformer addTransformer(
		final String name,
		final @Nullable String desc,
		final boolean obfuscated,
		final MethodNodeTransformer... transformers
	)
	{
		final MethodDescriptor md = new MethodDescriptor(this.internalName, name, desc, obfuscated);
		this.transformers.put(md.name, new ImmutablePair<>(md, transformers));

		return this;
	}

	public ClassVisitor createCV(final int api, final ClassVisitor cv)
	{
		return new ClassVisitor(api, cv)
		{
			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
			{
				final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

				final Collection<Pair<MethodDescriptor, MethodNodeTransformer[]>> candidates = MethodTransformer.this.transformers.get(name);

				if (!candidates.isEmpty())
				{
					List<MethodNodeTransformer[]> matches = null;

					for (final Pair<MethodDescriptor, MethodNodeTransformer[]> candidate : candidates)
					{
						final MethodDescriptor md = candidate.getKey();

						if (md.desc == null || md.desc.equals(desc))
						{
							if (matches == null)
								matches = new ArrayList<>();

							matches.add(candidate.getValue());
						}
					}

					if (matches != null)
						return MethodTransformer.this.transformMV(mv, matches, access, name, desc, signature, exceptions);
				}

				return mv;
			}
		};
	}

	private MethodVisitor transformMV(final MethodVisitor mv_, final List<MethodNodeTransformer[]> transformers, final int access, final String name, final String desc, final String signature, final String[] exceptions)
	{
		return new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions)
		{
			@Override
			public void visitEnd()
			{
				super.visitEnd();

				MethodNode methodNode = this;

				try
				{
					for (final MethodNodeTransformer[] transformers_ : transformers)
						for (final MethodNodeTransformer transformer : transformers_)
							methodNode = transformer.transform(MethodTransformer.this.internalName, methodNode, MethodTransformer.this.logger);

					if (MethodTransformer.this.verify)
					{
						final Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());

						analyzer.analyze(MethodTransformer.this.internalName, methodNode);
					}
				} catch (final MethodTransformerException | AnalyzerException e)
				{
					final String msg = "Error while modifying method " + this.name + " of class " + MethodTransformer.this.internalName;

					MethodTransformer.this.logger.error(msg, e);

					throw new IllegalArgumentException(msg, e);
				}

				methodNode.accept(mv_);
			}
		};
	}
}
