/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.imagej.ops.Initializable;
import net.imagej.ops.Op;
import net.imagej.ops.OpCandidate;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.OpRef;
import net.imagej.ops.Threadable;
import net.imagej.ops.special.computer.BinaryComputerOp;
import net.imagej.ops.special.computer.NullaryComputerOp;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.NullaryFunctionOp;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.special.hybrid.BinaryHybridCF;
import net.imagej.ops.special.hybrid.BinaryHybridCFI;
import net.imagej.ops.special.hybrid.BinaryHybridCFI1;
import net.imagej.ops.special.hybrid.NullaryHybridCF;
import net.imagej.ops.special.hybrid.UnaryHybridCF;
import net.imagej.ops.special.hybrid.UnaryHybridCFI;
import net.imagej.ops.special.hybrid.UnaryHybridCI;
import net.imagej.ops.special.inplace.BinaryInplaceOp;
import net.imagej.ops.special.inplace.UnaryInplaceOp;

import org.scijava.InstantiableException;

/**
 * A <em>special</em> operation is one intended to be used repeatedly from other
 * ops. Such reuse provides additional type safety and performance gains over
 * calling the ops matching engine (i.e., the {@link OpEnvironment#run}
 * methods).
 * <p>
 * Special ops come in three major flavors: <em>computer</em>, <em>function</em>
 * and <em>inplace</em>. In addition, <em>hybrid</em> ops union together
 * <em>computer</em>, <em>function</em> and/or <em>inplace</em> in various
 * combinations.
 * </p>
 * <p>
 * There are three arities currently implemented: {@link NullaryOp},
 * {@link UnaryOp} and {@link BinaryOp}. These arities correspond to the number
 * of <em>typed</em> input parameters. Additional input parameters are allowed,
 * but not strongly typed at the interface level.
 * </p>
 * <p>
 * The following table summarizes the available kinds of special ops:
 * </p>
 * <table style="border: 1px solid black; border-collapse: collapse">
 * <tr>
 * <th>Name</th>
 * <th>Summary</th>
 * <th>Stipulations</th>
 * <th style="white-space: nowrap">Output type</th>
 * <th>Arity</th>
 * <th>Class</th>
 * <th>Methods</th>
 * </tr>
 * <tr style="border-top: 1px solid gray">
 * <th rowspan=3>computer</th>
 * <td style="vertical-align: top" rowspan=3>An op which computes a result from
 * the given input I, storing the result into the specified preallocated output
 * reference O.</td>
 * <td style="vertical-align: top" rowspan=3>
 * <ul>
 * <li>Mutating the input contents is not allowed.</li>
 * <li>The output and input references must be different (i.e., computers do not
 * work in-place; see <em>inplace</em> below)</li>
 * <li>The output's initial contents must not affect the value of the result.
 * </li>
 * </ul>
 * </td>
 * <td rowspan=3>BOTH</td>
 * <td>0</td>
 * <td>{@link NullaryComputerOp}</th>
 * <td>{@code void compute0(O)}</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>{@link UnaryComputerOp}</th>
 * <td>{@code void compute1(O, I)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryComputerOp}</th>
 * <td>{@code void compute2(O, I1, I2)}</td>
 * </tr>
 * <tr style="border-top: 1px solid gray">
 * <th rowspan=3>function</th>
 * <td style="vertical-align: top" rowspan=3>An op which computes a result from
 * the given input I, returning the result as a newly allocated output O.</td>
 * <td style="vertical-align: top" rowspan=3>
 * <ul>
 * <li>Mutating the input contents is not allowed.</li>
 * </ul>
 * </td>
 * <td rowspan=3>OUTPUT</td>
 * <td>0</td>
 * <td>{@link NullaryFunctionOp}</th>
 * <td>{@code O compute0()}</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>{@link UnaryFunctionOp}</th>
 * <td>{@code O compute1(I)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryFunctionOp}</th>
 * <td>{@code O compute2(I1, I2)}</td>
 * </tr>
 * <tr style="border-top: 1px solid gray">
 * <th rowspan=2>inplace</th>
 * <td rowspan=2 style="vertical-align: top">An op which mutates the contents of
 * its argument(s) in-place.</td>
 * <td rowspan=2 style="vertical-align: top">-</td>
 * <td rowspan=2>BOTH</td>
 * <td>1</td>
 * <td>{@link UnaryInplaceOp}</th>
 * <td>{@code void mutate(A)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryInplaceOp}</th>
 * <td>{@code void mutate(A, A)}</td>
 * </tr>
 * <tr style="border-top: 3px double gray">
 * <th rowspan=3>hybrid CF</th>
 * <td style="vertical-align: top" rowspan=3>An op which is capable of behaving
 * as either a <em>computer</em> or as a <em>function</em>, providing the API
 * for both.</td>
 * <td style="vertical-align: top" rowspan=3>Same as <em>computer</em> and
 * <em>function</em> respectively.</td>
 * <td rowspan=3>BOTH (optional)</td>
 * <td>0</td>
 * <td>{@link NullaryHybridCF}</th>
 * <td style="white-space: nowrap">{@code void compute0(O)} +
 * {@code O compute0()}</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>{@link UnaryHybridCF}</th>
 * <td style="white-space: nowrap">{@code void compute1(O, I)} +
 * {@code O compute1(I)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryHybridCF}</th>
 * <td style="white-space: nowrap">{@code O compute1(I1, I2)} +
 * {@code void compute2(O, I1, I2)}</td>
 * </tr>
 * <tr style="border-top: 1px solid gray">
 * <th rowspan=3>hybrid CI</th>
 * <td style="vertical-align: top" rowspan=3>An op which is capable of behaving
 * as either a <em>computer</em> or an <em>inplace</em>, providing the API for
 * both.</td>
 * <td style="vertical-align: top" rowspan=3>Same as <em>computer</em> and
 * <em>inplace</em> respectively.</td>
 * <td rowspan=3>BOTH (optional)</td>
 * <td>1</td>
 * <td>{@link UnaryHybridCI}</th>
 * <td style="white-space: nowrap">{@code void compute1(A, A)} +
 * {@code A compute1(A)} + {@code void mutate(A)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@code BinaryHybridCI1} (unimplemented)</th>
 * <td style="white-space: nowrap">{@code void compute2(A, I, A)} +
 * {@code void mutate1(A, I)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@code BinaryHybridCI} (unimplemented)</th>
 * <td style="white-space: nowrap">{@code void compute2(A, A, A)} +
 * {@code void mutate1(A, A)} + {@code void mutate2(A, A)}</td>
 * </tr>
 * <tr style="border-top: 1px solid gray">
 * <th rowspan=3>hybrid CFI</th>
 * <td style="vertical-align: top" rowspan=3>An op which is capable of behaving
 * as either a <em>computer</em>, a <em>function</em> or an <em>inplace</em>,
 * providing the API for all three.</td>
 * <td style="vertical-align: top" rowspan=3>Same as <em>computer</em>,
 * <em>function</em> and <em>inplace</em> respectively.</td>
 * <td rowspan=3>BOTH (optional)</td>
 * <td>1</td>
 * <td>{@link UnaryHybridCFI}</th>
 * <td style="white-space: nowrap">{@code void compute1(A, A)} +
 * {@code A compute1(A)} + {@code void mutate(A)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryHybridCFI1}</th>
 * <td style="white-space: nowrap">{@code void compute2(A, I, A)} +
 * {@code A compute2(A, I)} + {@code void mutate1(A, I)}</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>{@link BinaryHybridCFI}</th>
 * <td style="white-space: nowrap">{@code void compute2(A, A, A)} +
 * {@code A compute2(A, A)} + {@code void mutate1(A, A)} +
 * {@code void mutate2(A, A)}</td>
 * </tr>
 * </table>
 * <p>
 * It is allowed for ops to implement multiple special op types. For example, an
 * op may implement {@link UnaryComputerOp} as well as {@link UnaryInplaceOp},
 * providing the option to compute the result in-place (saving memory) or into a
 * preallocated output reference (preserving the contents of the original input,
 * at the expense of memory).
 * </p>
 * 
 * @author Curtis Rueden
 */
public interface SpecialOp extends Op, Initializable, Threadable {

	/**
	 * Gets the op's number of special input parameters.
	 * <p>
	 * Note that this value may be larger than intuition might dictate in certain
	 * scenarios, because {@link UnaryOp} extends {@link NullaryOp}, and
	 * {@link BinaryOp} extends {@link UnaryOp}. This allows higher-order ops to
	 * be treated as lower-order by holding the extra input parameters constant.
	 * But it also means that e.g. a {@link BinaryComputerOp} which is locally
	 * typed as {@link UnaryComputerOp} will report its arity as 2 rather than 1
	 * as one might expect.
	 * </p>
	 */
	int getArity();

	// -- Threadable methods --

	@Override
	default SpecialOp getIndependentInstance() {
		// NB: We assume the op instance is thread-safe by default.
		// Individual implementations can override this assumption if they
		// have state (such as buffers) that cannot be shared across threads.
		return this;
	}

	// -- Utility methods --

	/**
	 * Gets the best {@link SpecialOp} implementation for the given types and
	 * arguments, populating its inputs.
	 *
	 * @param ops The {@link OpEnvironment} to search for a matching op.
	 * @param opType The {@link Class} of the operation. If multiple
	 *          {@link SpecialOp}s share this type (e.g., the type is an interface
	 *          which multiple {@link SpecialOp}s implement), then the best
	 *          {@link SpecialOp} implementation to use will be selected
	 *          automatically from the type and arguments.
	 * @param specialType The {@link SpecialOp} type to which matches should be
	 *          restricted.
	 * @param outType the type of the op's primary output, or null for any type.
	 * @param args The operation's arguments.
	 * @return A typed {@link SpecialOp} with populated inputs, ready to use.
	 */
	static <OP extends Op, S extends SpecialOp, O> S op(final OpEnvironment ops,
		final Class<OP> opType, final Class<S> specialType, final Class<O> outType,
		final Object... args)
	{
		final OpRef<OP> ref = OpRef.createTypes(opType, specialType, outType, args);
		@SuppressWarnings("unchecked")
		final S op = (S) ops.op(ref);
		return op;
	}

	static <OP extends Op> List<OpCandidate<OP>> candidates(
		final OpEnvironment ops, final String name, final Class<OP> opType,
		final int arity, final Flavor flavor)
	{
		// look up matching candidates
		final Class<?> specialType;
		if (flavor == Flavor.COMPUTER) specialType = NullaryComputerOp.class;
		else if (flavor == Flavor.FUNCTION) specialType = NullaryFunctionOp.class;
		else if (flavor == Flavor.INPLACE) specialType = UnaryInplaceOp.class;
		else specialType = null;
		final Set<? extends Class<?>> specialTypes = specialType == null ? null
			: Collections.singleton(specialType);
		final OpRef<OP> ref = new OpRef<>(name, opType, specialTypes, null);
		return filterArity(ops.matcher().findCandidates(ops, ref), arity);
	}

	/** Extracts a sublist of op candidates with a particular arity. */
	static <OP extends Op> List<OpCandidate<OP>> filterArity(
		final List<OpCandidate<OP>> candidates, final int arity)
	{
		if (arity < 0) return candidates;
		final ArrayList<OpCandidate<OP>> matches = new ArrayList<>();
		for (final OpCandidate<OP> candidate : candidates) {
			try {
				final Class<?> opClass = candidate.cInfo().loadClass();
				final Object o = opClass.newInstance();
				if (!(o instanceof SpecialOp)) continue;
				final SpecialOp op = (SpecialOp) o;
				if (arity == op.getArity()) matches.add(candidate);
			}
			catch (final InstantiableException | InstantiationException
					| IllegalAccessException exc)
			{
				// NB: Ignore this problematic op.
			}
		}
		return matches;
	}

	// -- Enums --

	/** An enumeration of the primary kinds of special ops. */
	enum Flavor {
			COMPUTER, FUNCTION, INPLACE
	}

}
