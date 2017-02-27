/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 0.9.5.3</a>, using an XML
 * Schema.
 * $Id: ProteinNames.java 11485 2006-09-18 16:31:40Z marko $
 */

package textractor.xml.yapex.xml;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.util.ArrayList;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class ProteinNames.
 * 
 * @version $Revision: 11485 $ $Date: 2006-09-18 12:31:40 -0400 (Mon, 18 Sep 2006) $
 */
public class ProteinNames implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _nameList
     */
    private java.util.ArrayList _nameList;


      //----------------/
     //- Constructors -/
    //----------------/

    public ProteinNames() {
        super();
        _nameList = new ArrayList();
    } //-- textractor.xml.yapex.xml.ProteinNames()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addName
     * 
     * @param vName
     */
    public void addName(final java.lang.String vName)
        throws java.lang.IndexOutOfBoundsException
    {
        _nameList.add(vName);
    } //-- void addName(java.lang.String) 

    /**
     * Method addName
     * 
     * @param index
     * @param vName
     */
    public void addName(final int index, final java.lang.String vName)
        throws java.lang.IndexOutOfBoundsException
    {
        _nameList.add(index, vName);
    } //-- void addName(int, java.lang.String) 

    /**
     * Method clearName
     */
    public void clearName()
    {
        _nameList.clear();
    } //-- void clearName() 

    /**
     * Method enumerateName
     */
    public java.util.Enumeration enumerateName()
    {
        return new org.exolab.castor.util.IteratorEnumeration(_nameList.iterator());
    } //-- java.util.Enumeration enumerateName() 

    /**
     * Method getName
     * 
     * @param index
     */
    public java.lang.String getName(final int index)
        throws java.lang.IndexOutOfBoundsException
    {
        //-- check bounds for index
        if ((index < 0) || (index > _nameList.size())) {
            throw new IndexOutOfBoundsException();
        }
        
        return (String)_nameList.get(index);
    } //-- java.lang.String getName(int) 

    /**
     * Method getName
     */
    public java.lang.String[] getName()
    {
        final int size = _nameList.size();
        final java.lang.String[] mArray = new java.lang.String[size];
        for (int index = 0; index < size; index++) {
            mArray[index] = (String)_nameList.get(index);
        }
        return mArray;
    } //-- java.lang.String[] getName() 

    /**
     * Method getNameCount
     */
    public int getNameCount()
    {
        return _nameList.size();
    } //-- int getNameCount() 

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
     * Method removeName
     * 
     * @param vName
     */
    public boolean removeName(final java.lang.String vName)
    {
        final boolean removed = _nameList.remove(vName);
        return removed;
    } //-- boolean removeName(java.lang.String) 

    /**
     * Method setName
     * 
     * @param index
     * @param vName
     */
    public void setName(final int index, final java.lang.String vName)
        throws java.lang.IndexOutOfBoundsException
    {
        //-- check bounds for index
        if ((index < 0) || (index > _nameList.size())) {
            throw new IndexOutOfBoundsException();
        }
        _nameList.set(index, vName);
    } //-- void setName(int, java.lang.String) 

    /**
     * Method setName
     * 
     * @param nameArray
     */
    public void setName(final java.lang.String[] nameArray)
    {
        //-- copy array
        _nameList.clear();
        for (int i = 0; i < nameArray.length; i++) {
            _nameList.add(nameArray[i]);
        }
    } //-- void setName(java.lang.String) 

    /**
     * Method unmarshal
     * 
     * @param reader
     */
    public static java.lang.Object unmarshal(final java.io.Reader reader)
        throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException
    {
        return Unmarshaller.unmarshal(textractor.xml.yapex.xml.ProteinNames.class, reader);
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
