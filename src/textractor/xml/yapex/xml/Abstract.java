/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 0.9.5.3</a>, using an XML
 * Schema.
 * $Id: Abstract.java 11485 2006-09-18 16:31:40Z marko $
 */

package textractor.xml.yapex.xml;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Abstract.
 * 
 * @version $Revision: 11485 $ $Date: 2006-09-18 12:31:40 -0400 (Mon, 18 Sep 2006) $
 */
public class Abstract implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _pmid
     */
    private long _pmid;

    /**
     * keeps track of state for field: _pmid
     */
    private boolean _has_pmid;

    /**
     * Field _text
     */
    private java.lang.String _text;

    /**
     * Field _proteinNames
     */
    private textractor.xml.yapex.xml.ProteinNames _proteinNames;


      //----------------/
     //- Constructors -/
    //----------------/

    public Abstract() {
        super();
    } //-- textractor.xml.yapex.xml.Abstract()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method deletePmid
     */
    public void deletePmid()
    {
        this._has_pmid= false;
    } //-- void deletePmid() 

    /**
     * Returns the value of field 'pmid'.
     * 
     * @return the value of field 'pmid'.
     */
    public long getPmid()
    {
        return this._pmid;
    } //-- long getPmid() 

    /**
     * Returns the value of field 'proteinNames'.
     * 
     * @return the value of field 'proteinNames'.
     */
    public textractor.xml.yapex.xml.ProteinNames getProteinNames()
    {
        return this._proteinNames;
    } //-- textractor.xml.yapex.xml.ProteinNames getTermsByClass()

    /**
     * Returns the value of field 'text'.
     * 
     * @return the value of field 'text'.
     */
    public java.lang.String getText()
    {
        return this._text;
    } //-- java.lang.String getText() 

    /**
     * Method hasPmid
     */
    public boolean hasPmid()
    {
        return this._has_pmid;
    } //-- boolean hasPmid() 

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
     * Sets the value of field 'pmid'.
     * 
     * @param pmid the value of field 'pmid'.
     */
    public void setPmid(final long pmid)
    {
        this._pmid = pmid;
        this._has_pmid = true;
    } //-- void setPmid(long) 

    /**
     * Sets the value of field 'proteinNames'.
     * 
     * @param proteinNames the value of field 'proteinNames'.
     */
    public void setProteinNames(final textractor.xml.yapex.xml.ProteinNames proteinNames)
    {
        this._proteinNames = proteinNames;
    } //-- void setProteinNames(textractor.xml.yapex.xml.ProteinNames)

    /**
     * Sets the value of field 'text'.
     * 
     * @param text the value of field 'text'.
     */
    public void setText(final java.lang.String text)
    {
        this._text = text;
    } //-- void setText(java.lang.String) 

    /**
     * Method unmarshal
     * 
     * @param reader
     */
    public static java.lang.Object unmarshal(final java.io.Reader reader)
        throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException
    {
        return Unmarshaller.unmarshal(textractor.xml.yapex.xml.Abstract.class, reader);
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
