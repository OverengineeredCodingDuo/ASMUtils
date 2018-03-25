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

import ocd.asmutil.matchers.MethodMatcher.MethodDescriptor;

public class MethodTransformer implements IClassTransformer
{
	private final Multimap<String, Pair<MethodDescriptor, MethodNodeTransformer[]>> transformers = ArrayListMultimap.create();

	public MethodTransformer addTransformer(
		final MethodDescriptor md,
		final MethodNodeTransformer... transformers
	)
	{
		this.transformers.put(md.name, new ImmutablePair<>(md, transformers));

		return this;
	}

	public MethodTransformer addTransformer(
		final String name,
		final @Nullable String desc,
		final MethodNodeTransformer... transformers
	)
	{
		return this.addTransformer(new MethodDescriptor(null, name, desc), transformers);
	}

	public static class Named extends MethodTransformer implements IClassTransformer.Named
	{
		private final String name;

		public Named(final String name)
		{
			this.name = name;
		}

		@Override
		public String getName()
		{
			return this.name;
		}
	}

	@Override
	public ClassVisitor createClassVisitor(final Logger logger, final boolean verify, final int api, final ClassVisitor cv)
	{
		return new ClassVisitor(api, cv)
		{
			private String internalName;

			@Override
			public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces)
			{
				super.visit(version, access, name, signature, superName, interfaces);

				this.internalName = name;
			}

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

						if (md.matches(this.internalName, name, desc))
						{
							if (matches == null)
								matches = new ArrayList<>();

							matches.add(candidate.getValue());
						}
					}

					if (matches != null)
						return MethodTransformer.this.createMethodVisitor(
							logger,
							verify,
							this.internalName,
							mv,
							matches,
							access,
							name,
							desc,
							signature,
							exceptions
						);
				}

				return mv;
			}
		};
	}

	private MethodVisitor createMethodVisitor(
		final Logger logger,
		final boolean verify,
		final String internalName,
		final MethodVisitor mv_,
		final List<MethodNodeTransformer[]> transformers,
		final int access,
		final String name,
		final String desc,
		final String signature,
		final String[] exceptions
	)
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
							methodNode = transformer.transform(internalName, methodNode, logger);

					if (verify)
					{
						final Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());

						analyzer.analyze(internalName, methodNode);
					}
				} catch (final MethodTransformerException | AnalyzerException e)
				{
					final String msg = "Error while modifying method " + this.name + " of class " + internalName;

					logger.fatal(msg, e);

					throw new IllegalArgumentException(msg, e);
				}

				methodNode.accept(mv_);
			}
		};
	}
}
