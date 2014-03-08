/*
 * #%L
 * ImageJ OPS: a framework for reusable algorithms.
 * %%
 * Copyright (C) 2014 Board of Regents of the University of
 * Wisconsin-Madison and University of Konstanz.
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

package imagej.ops.tests;

import static org.junit.Assert.assertTrue;
import imagej.module.Module;
import imagej.ops.Function;
import imagej.ops.map.Mapper;
import imagej.ops.map.MapperII;
import imagej.ops.map.ThreadedMapper;
import imagej.ops.map.ThreadedMapperII;
import imagej.ops.map.ThreadedInplaceMapperII;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing multi threaded implementation ({@link ThreadedMapper} and
 * {@link ThreadedMapperII}) of the mappers. Assumption: Naive Implementation of
 * {@link Mapper} works fine.
 * 
 * @author Christian Dietz
 */
public class ThreadedMapperTests extends AbstractOpTest {

	private Img<ByteType> in;

	@Before
	public void initImg() {
		in = generateByteTestImg(true, 10, 10);
	}

	@Test
	public void testMultiThreadedMapper() {

		final Module naiveMapper = ops.module(new MapperII<ByteType, ByteType>());
		final Img<ByteType> outNaive = generateByteTestImg(false, 10, 10);
		naiveMapper.setInput("func", new DummyPixelOp<ByteType, ByteType>());
		naiveMapper.setInput("in", in);
		naiveMapper.setInput("out", outNaive);

		naiveMapper.run();

		final Module threadedMapper =
			ops.module(new ThreadedMapper<ByteType, ByteType>());
		final Img<ByteType> outThreaded = generateByteTestImg(false, 10, 10);
		threadedMapper.setInput("func", new DummyPixelOp<ByteType, ByteType>());
		threadedMapper.setInput("in", in);
		threadedMapper.setInput("out", outThreaded);

		threadedMapper.run();

		final Module threadedMapperII =
			ops.module(new ThreadedMapperII<ByteType, ByteType>());
		final Img<ByteType> outThreadedII = generateByteTestImg(false, 10, 10);
		threadedMapperII.setInput("func", new DummyPixelOp<ByteType, ByteType>());
		threadedMapperII.setInput("in", in);
		threadedMapperII.setInput("out", outThreadedII);

		threadedMapperII.run();

		final Module threadedMapperInplaceII =
			ops.module(new ThreadedInplaceMapperII<ByteType>());
		final Img<ByteType> outThreadedInplaceII =
			generateByteTestImg(false, 10, 10);

		threadedMapperII.setInput("func", new DummyPixelOp<ByteType, ByteType>());
		threadedMapperII.setInput("in", in);

		threadedMapperInplaceII.run();

		final Cursor<ByteType> cursor1 = outNaive.cursor();
		final Cursor<ByteType> cursor2 = outThreaded.cursor();
		final Cursor<ByteType> cursor3 = outThreadedII.cursor();
		final Cursor<ByteType> cursor4 = outThreadedInplaceII.cursor();

		// test for consistency as we know that naiveMapper works.
		while (cursor1.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			cursor3.fwd();
			cursor4.fwd();

			assertTrue(cursor1.get().get() == cursor2.get().get());
			assertTrue(cursor1.get().get() == cursor3.get().get());
			assertTrue(cursor1.get().get() == cursor4.get().get());
		}
	}

	private class DummyPixelOp<T extends RealType<T>, V extends RealType<V>>
		extends Function<T, V>
	{

		double constant = -5;

		@Override
		public V compute(final T input, final V output) {
			output.setReal(input.getRealDouble() + constant);
			return output;
		}

		@Override
		public Function<T, V> copy() {
			return new DummyPixelOp<T, V>();
		}
	}
}