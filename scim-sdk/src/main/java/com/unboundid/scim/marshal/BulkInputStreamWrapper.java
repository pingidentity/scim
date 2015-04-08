/*
 * Copyright 2012-2015 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.marshal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;



/**
 * This class is a wrapper around an input stream that allows us to determine
 * how many bytes have been read from the stream.
 */
public class BulkInputStreamWrapper extends FilterInputStream
{
  // The number of bytes read from the stream.
  private final AtomicLong bytesRead;



  /**
   * Creates a new instance of this input stream that wraps the provided
   * stream.
   *
   * @param  s  The input stream to be wrapped.
   */
  public BulkInputStreamWrapper(final InputStream s)
  {
    super(s);
    bytesRead  = new AtomicLong(0L);
  }



  /**
   * Reads the next byte of data from the input stream. The value byte is
   * returned as an <code>int</code> in the range <code>0</code> to
   * <code>255</code>. If no byte is available because the end of the stream has
   * been reached, the value <code>-1</code> is returned. This method blocks
   * until input data is available, the end of the stream is detected, or an
   * exception is thrown.
   *
   * <p> A subclass must provide an implementation of this method.
   *
   * @return the next byte of data, or <code>-1</code> if the end of the stream
   *         is reached.
   *
   * @throws java.io.IOException if an I/O error occurs.
   */
  @Override
  public int read() throws IOException
  {
    int c = in.read();
    if (c != -1)
    {
      bytesRead.incrementAndGet();
    }

    return c;
  }



  /**
   * Reads some number of bytes from the input stream and stores them into the
   * buffer array <code>b</code>. The number of bytes actually read is returned
   * as an integer.  This method blocks until input data is available, end of
   * file is detected, or an exception is thrown.
   *
   * <p> If the length of <code>b</code> is zero, then no bytes are read and
   * <code>0</code> is returned; otherwise, there is an attempt to read at least
   * one byte. If no byte is available because the stream is at the end of the
   * file, the value <code>-1</code> is returned; otherwise, at least one byte
   * is read and stored into <code>b</code>.
   *
   * <p> The first byte read is stored into element <code>b[0]</code>, the next
   * one into <code>b[1]</code>, and so on. The number of bytes read is, at
   * most, equal to the length of <code>b</code>. Let <i>k</i> be the number of
   * bytes actually read; these bytes will be stored in elements
   * <code>b[0]</code> through <code>b[</code><i>k</i><code>-1]</code>, leaving
   * elements <code>b[</code><i>k</i><code>]</code> through
   * <code>b[b.length-1]</code> unaffected.
   *
   * <p> The <code>read(b)</code> method for class <code>InputStream</code>
   * has the same effect as: <pre><code> read(b, 0, b.length) </code></pre>
   *
   * @param b the buffer into which the data is read.
   *
   * @return the total number of bytes read into the buffer, or <code>-1</code>
   *         is there is no more data because the end of the stream has been
   *         reached.
   *
   * @throws java.io.IOException  If the first byte cannot be read for any
   *                              reason other than the end of the file, if the
   *                              input stream has been closed, or if some other
   *                              I/O error occurs.
   * @see java.io.InputStream#read(byte[], int, int)
   */
  @Override
  public int read(final byte[] b) throws IOException
  {
    int n = in.read(b);
    if (n != -1)
    {
      bytesRead.addAndGet(n);
    }

    return n;
  }



  /**
   * Reads up to <code>len</code> bytes of data from the input stream into an
   * array of bytes.  An attempt is made to read as many as <code>len</code>
   * bytes, but a smaller number may be read. The number of bytes actually read
   * is returned as an integer.
   *
   * <p> This method blocks until input data is available, end of file is
   * detected, or an exception is thrown.
   *=
   * <p> If <code>len</code> is zero, then no bytes are read and <code>0</code>
   * is returned; otherwise, there is an attempt to read at least one byte. If
   * no byte is available because the stream is at end of file, the value
   * <code>-1</code> is returned; otherwise, at least one byte is read and
   * stored into <code>b</code>.
   *
   * <p> The first byte read is stored into element <code>b[off]</code>, the
   * next one into <code>b[off+1]</code>, and so on. The number of bytes read
   * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of bytes
   * actually read; these bytes will be stored in elements <code>b[off]</code>
   * through <code>b[off+</code><i>k</i><code>-1]</code>, leaving elements
   * <code>b[off+</code><i>k</i><code>]</code> through <code>b[off+len-1]</code>
   * unaffected.
   *
   * <p> In every case, elements <code>b[0]</code> through <code>b[off]</code>
   * and elements <code>b[off+len]</code> through <code>b[b.length-1]</code> are
   * unaffected.
   *
   * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method for
   * class <code>InputStream</code> simply calls the method <code>read()</code>
   * repeatedly. If the first such call results in an <code>IOException</code>,
   * that exception is returned from the call to the <code>read(b,</code>
   * <code>off,</code> <code>len)</code> method.  If any subsequent call to
   * <code>read()</code> results in a <code>IOException</code>, the exception is
   * caught and treated as if it were end of file; the bytes read up to that
   * point are stored into <code>b</code> and the number of bytes read before
   * the exception occurred is returned. The default implementation of this
   * method blocks until the requested amount of input data <code>len</code> has
   * been read, end of file is detected, or an exception is thrown. Subclasses
   * are encouraged to provide a more efficient implementation of this method.
   *
   * @param b   the buffer into which the data is read.
   * @param off the start offset in array <code>b</code> at which the data is
   *            written.
   * @param len the maximum number of bytes to read.
   *
   * @return the total number of bytes read into the buffer, or <code>-1</code>
   *         if there is no more data because the end of the stream has been
   *         reached.
   *
   * @throws java.io.IOException       If the first byte cannot be read for any
   *                                   reason other than end of file, or if the
   *                                   input stream has been closed, or if some
   *                                   other I/O error occurs.
   * @see java.io.InputStream#read()
   */
  @Override
  public int read(final byte[] b, final int off, final int len)
      throws IOException
  {
    int n = in.read(b, off, len);
    if (n != -1)
    {
      bytesRead.addAndGet(n);
    }

    return n;
  }



  /**
   * Skips over and discards <code>n</code> bytes of data from this input
   * stream.
   * The <code>skip</code> method may, for a variety of reasons, end up skipping
   * over some smaller number of bytes, possibly <code>0</code>. This may result
   * from any of a number of conditions; reaching end of file before
   * <code>n</code> bytes have been skipped is only one possibility. The actual
   * number of bytes skipped is returned.  If <code>n</code> is negative, no
   * bytes are skipped.
   *
   *
   * @param n the number of bytes to be skipped.
   *
   * @return the actual number of bytes skipped.
   *
   * @throws java.io.IOException if the stream does not support seek, or if some
   *                             other I/O error occurs.
   */
  @Override
  public long skip(final long n) throws IOException
  {
    long skipped = in.skip(n);
    bytesRead.addAndGet(skipped);

    return n;
  }



  /**
   * Retrieves the number of bytes read through this input stream.
   *
   * @return  The number of bytes read through this input stream.
   */
  public long getBytesRead()
  {
    return bytesRead.get();
  }
}
