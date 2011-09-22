/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;



/**
 * This class represents a Simple Cloud Identity Management attribute type,
 * comprised of the schema the attribute belongs to and the name of the
 * attribute.
 */
public final class SCIMAttributeType
{
  /**
   * The URI of the schema to which this attribute belongs.
   */
  private final String schema;

  /**
   * The name of the attribute.
   */
  private final String name;

  /**
   * The normalized lower case name of the attribute.
   */
  private final String normalizedName;

  /**
   * The qualified name of the attribute.
   */
  private final String qualifiedName;



  /**
   * Create a new SCIM attribute type from the provided information.
   *
   * @param schema  The URI of the schema to which this attribute belongs.
   * @param name    The name of the attribute.
   *
   */
  public SCIMAttributeType(final String schema, final String name)
  {
    this.schema = schema;
    this.name   = name;
    this.normalizedName = name.toLowerCase();
    this.qualifiedName  = buildQualifiedName();
  }



  /**
   * Create a new SCIM core attribute type.
   *
   * @param name    The name of the attribute from the core schema.
   *
   */
  public SCIMAttributeType(final String name)
  {
    this(SCIMConstants.SCHEMA_URI_CORE, name);
  }



  /**
   * Create a new SCIM attribute type from the qualified name of the attribute.
   *
   * @param qualifiedName  The qualified name of the attribute.
   *
   * @return  The SCIM attribute type.
   */
  public static SCIMAttributeType fromQualifiedName(final String qualifiedName)
  {
    final int lastColonPos =
        qualifiedName.lastIndexOf(
            SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
    if (lastColonPos == -1)
    {
      return new SCIMAttributeType(qualifiedName);
    }
    else
    {
      return new SCIMAttributeType(qualifiedName.substring(0, lastColonPos),
                                   qualifiedName.substring(lastColonPos+1));
    }
  }



  /**
   * Retrieve the name of the schema to which this attribute belongs.
   *
   * @return  The name of the schema to which this attribute belongs.
   */
  public String getSchema()
  {
    return schema;
  }



  /**
   * Retrieve the name of this attribute. The name does not indicate which
   * schema the attribute belongs to.
   *
   * @return  The name of this attribute.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Returns the qualified name of this attribute type.
   *
   * @return  The qualified name of this attribute type.
   */
  public String toQualifiedName()
  {
    return qualifiedName;
  }



  /**
   * {@inheritDoc}
   */
  public String toString()
  {
    return toQualifiedName();
  }



  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hashtables such as those provided by
   * <code>java.util.Hashtable</code>.
   * <p/>
   * The general contract of <code>hashCode</code> is:
   * <ul>
   * <li>Whenever it is invoked on the same object more than once during
   * an execution of a Java application, the <tt>hashCode</tt> method
   * must consistently return the same integer, provided no information
   * used in <tt>equals</tt> comparisons on the object is modified.
   * This integer need not remain consistent from one execution of an
   * application to another execution of the same application.
   * <li>If two objects are equal according to the <tt>equals(Object)</tt>
   * method, then calling the <code>hashCode</code> method on each of
   * the two objects must produce the same integer result.
   * <li>It is <em>not</em> required that if two objects are unequal
   * according to the {@link Object#equals(Object)}
   * method, then calling the <tt>hashCode</tt> method on each of the
   * two objects must produce distinct integer results.  However, the
   * programmer should be aware that producing distinct integer results
   * for unequal objects may improve the performance of hashtables.
   * </ul>
   * <p/>
   * As much as is reasonably practical, the hashCode method defined by
   * class <tt>Object</tt> does return distinct integers for distinct
   * objects. (This is typically implemented by converting the internal
   * address of the object into an integer, but this implementation
   * technique is not required by Java.)
   *
   * @return a hash code value for this object.
   *
   * @see Object#equals(Object)
   * @see java.util.Hashtable
   */
  @Override
  public int hashCode()
  {
    int hashCode = 0;

    hashCode += schema.hashCode();
    hashCode += normalizedName.hashCode();

    return hashCode;
  }



  /**
   * Indicates whether some other object is "equal to" this one.
   * <p/>
   * The <code>equals</code> method implements an equivalence relation
   * on non-null object references:
   * <ul>
   * <li>It is <i>reflexive</i>: for any non-null reference value
   * <code>x</code>, <code>x.equals(x)</code> should return
   * <code>true</code>.
   * <li>It is <i>symmetric</i>: for any non-null reference values
   * <code>x</code> and <code>y</code>, <code>x.equals(y)</code>
   * should return <code>true</code> if and only if
   * <code>y.equals(x)</code> returns <code>true</code>.
   * <li>It is <i>transitive</i>: for any non-null reference values
   * <code>x</code>, <code>y</code>, and <code>z</code>, if
   * <code>x.equals(y)</code> returns <code>true</code> and
   * <code>y.equals(z)</code> returns <code>true</code>, then
   * <code>x.equals(z)</code> should return <code>true</code>.
   * <li>It is <i>consistent</i>: for any non-null reference values
   * <code>x</code> and <code>y</code>, multiple invocations of
   * <tt>x.equals(y)</tt> consistently return <code>true</code>
   * or consistently return <code>false</code>, provided no
   * information used in <code>equals</code> comparisons on the
   * objects is modified.
   * <li>For any non-null reference value <code>x</code>,
   * <code>x.equals(null)</code> should return <code>false</code>.
   * </ul>
   * <p/>
   * The <tt>equals</tt> method for class <code>Object</code> implements
   * the most discriminating possible equivalence relation on objects;
   * that is, for any non-null reference values <code>x</code> and
   * <code>y</code>, this method returns <code>true</code> if and only
   * if <code>x</code> and <code>y</code> refer to the same object
   * (<code>x == y</code> has the value <code>true</code>).
   * <p/>
   * Note that it is generally necessary to override the <tt>hashCode</tt>
   * method whenever this method is overridden, so as to maintain the
   * general contract for the <tt>hashCode</tt> method, which states
   * that equal objects must have equal hash codes.
   *
   * @param obj the reference object with which to compare.
   *
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   *
   * @see #hashCode()
   * @see java.util.Hashtable
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (!(obj instanceof SCIMAttributeType))
    {
      return false;
    }

    final SCIMAttributeType that = (SCIMAttributeType)obj;
    if (this.schema == null && that.schema == null)
    {
      return this.name.equalsIgnoreCase(that.name);
    }
    else if (this.schema == null)
    {
      return false;
    }
    else if (that.schema == null)
    {
      return false;
    }
    else
    {
      return this.schema.equals(that.schema) &&
             this.name.equalsIgnoreCase(that.name);
    }
  }



  /**
   * Builds the qualified name of the attribute.
   *
   * @return  The qualified name of the attribute.
   */
  public String buildQualifiedName()
  {
    final StringBuilder builder = new StringBuilder();

    if (!schema.equals(SCIMConstants.SCHEMA_URI_CORE))
    {
      builder.append(schema);
      builder.append(SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
    }

    builder.append(name);
    return builder.toString();
  }
}
