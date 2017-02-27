/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 0.9.5.3</a>, using an XML
 * Schema.
 * $Id: AbstractDescriptor.java 11485 2006-09-18 16:31:40Z marko $
 */

package textractor.xml.yapex.xml;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.validators.*;

/**
 * Class AbstractDescriptor.
 * 
 * @version $Revision: 11485 $ $Date: 2006-09-18 12:31:40 -0400 (Mon, 18 Sep 2006) $
 */
public class AbstractDescriptor extends org.exolab.castor.xml.util.XMLClassDescriptorImpl {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field nsPrefix
     */
    private java.lang.String nsPrefix;

    /**
     * Field nsURI
     */
    private java.lang.String nsURI;

    /**
     * Field xmlName
     */
    private java.lang.String xmlName;

    /**
     * Field identity
     */
    private org.exolab.castor.xml.XMLFieldDescriptor identity;


      //----------------/
     //- Constructors -/
    //----------------/

    public AbstractDescriptor() {
        super();
        xmlName = "abstract";
        
        //-- set grouping compositor
        setCompositorAsSequence();
        org.exolab.castor.xml.util.XMLFieldDescriptorImpl  desc           = null;
        org.exolab.castor.xml.XMLFieldHandler              handler        = null;
        org.exolab.castor.xml.FieldValidator               fieldValidator = null;
        //-- initialize attribute descriptors
        
        //-- initialize element descriptors
        
        //-- _pmid
        desc = new org.exolab.castor.xml.util.XMLFieldDescriptorImpl(long.class, "_pmid", "pmid", org.exolab.castor.xml.NodeType.Element);
        handler = (new org.exolab.castor.xml.XMLFieldHandler() {
            @Override
	    public java.lang.Object getValue( final java.lang.Object object ) 
                throws IllegalStateException
            {
                final Abstract target = (Abstract) object;
                if(!target.hasPmid()) {
		    return null;
		}
                return new java.lang.Long(target.getPmid());
            }
            @Override
	    public void setValue( final java.lang.Object object, final java.lang.Object value) 
                throws IllegalStateException, IllegalArgumentException
            {
                try {
                    final Abstract target = (Abstract) object;
                    // ignore null values for non optional primitives
                    if (value == null) {
			return;
		    }
                    
                    target.setPmid( ((java.lang.Long)value).longValue());
                }
                catch (final java.lang.Exception ex) {
                    throw new IllegalStateException(ex.toString());
                }
            }
            @Override
	    public java.lang.Object newInstance( final java.lang.Object parent ) {
                return null;
            }
        } );
        desc.setHandler(handler);
        desc.setRequired(true);
        desc.setMultivalued(false);
        addFieldDescriptor(desc);
        
        //-- validation code for: _pmid
        fieldValidator = new org.exolab.castor.xml.FieldValidator();
        fieldValidator.setMinOccurs(1);
        { //-- local scope
            final LongValidator typeValidator = new LongValidator();
            fieldValidator.setValidator(typeValidator);
        }
        desc.setValidator(fieldValidator);
        //-- _text
        desc = new org.exolab.castor.xml.util.XMLFieldDescriptorImpl(java.lang.String.class, "_text", "text", org.exolab.castor.xml.NodeType.Element);
        desc.setImmutable(true);
        handler = (new org.exolab.castor.xml.XMLFieldHandler() {
            @Override
	    public java.lang.Object getValue( final java.lang.Object object ) 
                throws IllegalStateException
            {
                final Abstract target = (Abstract) object;
                return target.getText();
            }
            @Override
	    public void setValue( final java.lang.Object object, final java.lang.Object value) 
                throws IllegalStateException, IllegalArgumentException
            {
                try {
                    final Abstract target = (Abstract) object;
                    target.setText( (java.lang.String) value);
                }
                catch (final java.lang.Exception ex) {
                    throw new IllegalStateException(ex.toString());
                }
            }
            @Override
	    public java.lang.Object newInstance( final java.lang.Object parent ) {
                return null;
            }
        } );
        desc.setHandler(handler);
        desc.setRequired(true);
        desc.setMultivalued(false);
        addFieldDescriptor(desc);
        
        //-- validation code for: _text
        fieldValidator = new org.exolab.castor.xml.FieldValidator();
        fieldValidator.setMinOccurs(1);
        { //-- local scope
            final StringValidator typeValidator = new StringValidator();
            typeValidator.setWhiteSpace("preserve");
            fieldValidator.setValidator(typeValidator);
        }
        desc.setValidator(fieldValidator);
        //-- _proteinNames
        desc = new org.exolab.castor.xml.util.XMLFieldDescriptorImpl(textractor.xml.yapex.xml.ProteinNames.class, "_proteinNames", "protein-names", org.exolab.castor.xml.NodeType.Element);
        handler = (new org.exolab.castor.xml.XMLFieldHandler() {
            @Override
	    public java.lang.Object getValue( final java.lang.Object object ) 
                throws IllegalStateException
            {
                final Abstract target = (Abstract) object;
                return target.getProteinNames();
            }
            @Override
	    public void setValue( final java.lang.Object object, final java.lang.Object value) 
                throws IllegalStateException, IllegalArgumentException
            {
                try {
                    final Abstract target = (Abstract) object;
                    target.setProteinNames( (textractor.xml.yapex.xml.ProteinNames) value);
                }
                catch (final java.lang.Exception ex) {
                    throw new IllegalStateException(ex.toString());
                }
            }
            @Override
	    public java.lang.Object newInstance( final java.lang.Object parent ) {
                return new textractor.xml.yapex.xml.ProteinNames();
            }
        } );
        desc.setHandler(handler);
        desc.setRequired(true);
        desc.setMultivalued(false);
        addFieldDescriptor(desc);
        
        //-- validation code for: _proteinNames
        fieldValidator = new org.exolab.castor.xml.FieldValidator();
        fieldValidator.setMinOccurs(1);
        { //-- local scope
        }
        desc.setValidator(fieldValidator);
    } //-- textractor.xml.yapex.xml.AbstractDescriptor()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method getAccessMode
     */
    @Override
    public org.exolab.castor.mapping.AccessMode getAccessMode()
    {
        return null;
    } //-- org.exolab.castor.mapping.AccessMode getAccessMode() 

    /**
     * Method getExtends
     */
    @Override
    public org.exolab.castor.mapping.ClassDescriptor getExtends()
    {
        return null;
    } //-- org.exolab.castor.mapping.ClassDescriptor getExtends() 

    /**
     * Method getIdentity
     */
    @Override
    public org.exolab.castor.mapping.FieldDescriptor getIdentity()
    {
        return identity;
    } //-- org.exolab.castor.mapping.FieldDescriptor getIdentity() 

    /**
     * Method getJavaClass
     */
    @Override
    public java.lang.Class getJavaClass()
    {
        return textractor.xml.yapex.xml.Abstract.class;
    } //-- java.lang.Class getJavaClass() 

    /**
     * Method getNameSpacePrefix
     */
    @Override
    public java.lang.String getNameSpacePrefix()
    {
        return nsPrefix;
    } //-- java.lang.String getNameSpacePrefix() 

    /**
     * Method getNameSpaceURI
     */
    @Override
    public java.lang.String getNameSpaceURI()
    {
        return nsURI;
    } //-- java.lang.String getNameSpaceURI() 

    /**
     * Method getValidator
     */
    @Override
    public org.exolab.castor.xml.TypeValidator getValidator()
    {
        return this;
    } //-- org.exolab.castor.xml.TypeValidator getValidator() 

    /**
     * Method getXMLName
     */
    @Override
    public java.lang.String getXMLName()
    {
        return xmlName;
    } //-- java.lang.String getXMLName() 

}
