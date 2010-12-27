package org.freeplane.features.common.filter.condition;

import java.lang.reflect.Method;

import javax.swing.JComponent;

import org.freeplane.n3.nanoxml.XMLElement;


public abstract class ASelectableCondition  implements ICondition{
	transient private String description;
	transient private JComponent renderer;
	private static Method EQUALS;
	private static Method HASH;
	static{
		try{
			final ClassLoader classLoader = ASelectableCondition.class.getClassLoader();
			EQUALS = classLoader.loadClass("org.apache.commons.lang.builder.EqualsBuilder").getMethod("reflectionEquals", Object.class, Object.class);
			HASH = classLoader.loadClass("org.apache.commons.lang.builder.HashCodeBuilder").getMethod("reflectionHashCode", Object.class);
		}
		catch(Exception e){
			
		}
	}

	public ASelectableCondition() {
		super();
	}


	@Override
    public int hashCode() {
		if(HASH == null){
			return super.hashCode();
		}
		try {
	        return (Integer) HASH.invoke(null, this);
        }
        catch (Exception e) {
	        e.printStackTrace();
	        return super.hashCode();
        }
    }

	@Override
    public boolean equals(Object obj) {
		if(EQUALS == null){
			return super.equals(obj);
		}
		try {
	        return (Boolean) EQUALS.invoke(null, this, obj);
        }
        catch (Exception e) {
	        e.printStackTrace();
	        return super.equals(obj);
        }
    }
	protected abstract String createDescription();
	
	final public JComponent getListCellRendererComponent() {
		if (renderer == null) {
			renderer = createRendererComponent();
		}
		return renderer;
	}

	protected JComponent createRendererComponent() {
	    return ConditionFactory.createCellRendererComponent(toString());
    }

	@Override
    final public String toString() {
    	if (description == null) {
    		description = createDescription();
    	}
    	return description;
    }
	
	public void toXml(final XMLElement element) {
		final XMLElement child = new XMLElement();
		child.setName(getName());
		fillXML(child);
		element.addChild(child);
	}

	protected void fillXML(XMLElement element){}

	abstract protected String getName();

}
