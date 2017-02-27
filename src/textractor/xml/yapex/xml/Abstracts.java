/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 0.9.5.3</a>, using an XML
 * Schema.
 * $Id: Abstracts.java 11485 2006-09-18 16:31:40Z marko $
 */

package textractor.xml.yapex.xml;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.util.ArrayList;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Abstracts.
 * 
 * @version $Revision: 11485 $ $Date: 2006-09-18 12:31:40 -0400 (Mon, 18 Sep 2006) $
 */
public class Abstracts implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _abstractList
     */
    private java.util.ArrayList _abstractList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Abstracts() {
        super();
        _abstractList = new ArrayList();
    } //-- textractor.xml.yapex.xml.Abstracts()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method add_abstract
     * 
     * @param v_abstract
     */
    public void add_abstract(final textractor.xml.yapex.xml.Abstract v_abstract)
        throws java.lang.IndexOutOfBoundsException
    {
        _abstractList.add(v_abstract);
    } //-- void add_abstract(textractor.xml.yapex.xml.Abstract)

    /**
     * Method add_abstract
     * 
     * @param index
     * @param v_abstract
     */
    public void add_abstract(final int index, final textractor.xml.yapex.xml.Abstract v_abstract)
        throws java.lang.IndexOutOfBoundsException
    {
        _abstractList.add(index, v_abstract);
    } //-- void add_abstract(int, textractor.xml.yapex.xml.Abstract)

    /**
     * Method clear_abstract
     */
    public void clear_abstract()
    {
        _abstractList.clear();
    } //-- void clear_abstract() 

    /**
     * Method enumerate_abstract
     */
    public java.util.Enumeration enumerate_abstract()
    {
        return new org.exolab.castor.util.IteratorEnumeration(_abstractList.iterator());
    } //-- java.util.Enumeration enumerate_abstract() 

    /**
     * Method get_abstract
     * 
     * @param index
     */
    public textractor.xml.yapex.xml.Abstract get_abstract(final int index)
        throws java.lang.IndexOutOfBoundsException
    {
        //-- check bounds for index
        if ((index < 0) || (index > _abstractList.size())) {
            throw new IndexOutOfBoundsException();
        }
        
        return (textractor.xml.yapex.xml.Abstract) _abstractList.get(index);
    } //-- textractor.xml.yapex.xml.Abstract get_abstract(int)

    /**
     * Method get_abstract
     */
    public textractor.xml.yapex.xml.Abstract[] get_abstract()
    {
        final int size = _abstractList.size();
        final textractor.xml.yapex.xml.Abstract[] mArray = new textractor.xml.yapex.xml.Abstract[size];
        for (int index = 0; index < size; index++) {
            mArray[index] = (textractor.xml.yapex.xml.Abstract) _abstractList.get(index);
        }
        return mArray;
    } //-- textractor.xml.yapex.xml.Abstract[] get_abstract()

    /**
     * Method get_abstractCount
     */
    public int get_abstractCount()
    {
        return _abstractList.size();
    } //-- int get_abstractCount() 

    /**
     * Method isValid
     */
    public boolean isValid()
    {
        try {
            validate();
        }
        catch (final org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    } //-- boolean isValid() 

    /**
     * Method marshal
     * 
     * @param out
     */
    public void marshal(final java.io.Writer out)
        throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException
    {
        
        Marshaller.marshal(this, out);
    } //-- void marshal(java.io.Writer) 

    /**
     * Method marshal
     * 
     * @param handler
     */
    public void marshal(final org.xml.sax.ContentHandler handler)
        throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException
    {
        
        Marshaller.marshal(this, handler);
    } //-- void marshal(org.xml.sax.ContentHandler) 

    /**
     * Method remove_abstract
     * 
     * @param v_abstract
     */
    public boolean remove_abstract(final textractor.xml.yapex.xml.Abstract v_abstract)
    {
        final boolean removed = _abstractList.remove(v_abstract);
        return removed;
    } //-- boolean remove_abstract(textractor.xml.yapex.xml.Abstract)

    /**
     * Method set_abstract
     * 
     * @param index
     * @param v_abstract
     */
    public void set_abstract(final int index, final textractor.xml.yapex.xml.Abstract v_abstract)
        throws java.lang.IndexOutOfBoundsException
    {
        //-- check bounds for index
        if ((index < 0) || (index > _abstractList.size())) {
            throw new IndexOutOfBoundsException();
        }
        _abstractList.set(index, v_abstract);
    } //-- void set_abstract(int, textractor.xml.yapex.xml.Abstract)

    /**
     * Method set_abstract
     * 
     * @param _abstractArray
     */
    public void set_abstract(final textractor.xml.yapex.xml.Abstract[] _abstractArray)
    {
        //-- copy array
        _abstractList.clear();
        for (int i = 0; i < _abstractArray.length; i++) {
            _abstractList.add(_abstractArray[i]);
        }
    } //-- void set_abstract(textractor.xml.yapex.xml.Abstract)

    /**
     * Method unmarshal
     * 
     * @param reader
     */
    public static java.lang.Object unmarshal(final java.io.Reader reader)
        throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException
    {
        return Unmarshaller.unmarshal(textractor.xml.yapex.xml.Abstracts.class, reader);
    } //-- java.lang.Object unmarshal(java.io.Reader) 

    /**
     * Method validate
     */
    public void validate()
        throws org.exolab.castor.xml.ValidationException
    {
        final org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    } //-- void validate() 

}
