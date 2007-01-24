/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.management;

import org.apache.qpid.AMQException;
import org.apache.qpid.server.registry.ApplicationRegistry;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * Provides implementation of the boilerplate ManagedObject interface. Most managed objects should find it useful
 * to extend this class rather than implementing ManagedObject from scratch.
 *
 */
public abstract class DefaultManagedObject extends StandardMBean implements ManagedObject
{
    private Class<?> _managementInterface;

    private String _typeName;

    protected DefaultManagedObject(Class<?> managementInterface, String typeName)
        throws NotCompliantMBeanException
    {
        super(managementInterface);
        _managementInterface = managementInterface;
        _typeName = typeName;
    }

    public String getType()
    {
        return _typeName;
    }

    public Class<?> getManagementInterface()
    {
        return _managementInterface;
    }

    public ManagedObject getParentObject()
    {
        return null;
    }

    public void register() throws AMQException
    {
        try
        {
            getManagedObjectRegistry().registerObject(this);
        }
        catch (JMException e)
        {
            throw new AMQException("Error registering managed object " + this + ": " + e, e);
        }
    }

    protected ManagedObjectRegistry getManagedObjectRegistry()
    {
        return ApplicationRegistry.getInstance().getManagedObjectRegistry();
    }

    public void unregister() throws AMQException
    {
        try
        {
            getManagedObjectRegistry().unregisterObject(this);
        }
        catch (JMException e)
        {
            throw new AMQException("Error unregistering managed object: " + this + ": " + e, e);
        }
    }

    public String toString()
    {
        return getObjectInstanceName() + "[" + getType() + "]";
    }
    

    /**
     * Created the ObjectName as per the JMX Specs
     * @return ObjectName
     * @throws MalformedObjectNameException
     */
    public ObjectName getObjectName()
        throws MalformedObjectNameException
    {
        String name = getObjectInstanceName();
        StringBuffer objectName = new StringBuffer(ManagedObject.DOMAIN);

        objectName.append(":type=");
        objectName.append(getHierarchicalType(this));

        objectName.append(",");
        objectName.append(getHierarchicalName(this));
        objectName.append("name=").append(name);

        return new ObjectName(objectName.toString());
    }

    private String getHierarchicalType(ManagedObject obj)
    {
        String parentType = null;
        if (obj.getParentObject() != null)
        {
            parentType = getHierarchicalType(obj.getParentObject()).toString();
            return parentType + "." + obj.getType();
        }
        else
            return obj.getType();
    }

    private String getHierarchicalName(ManagedObject obj)
    {
        String parentName = null;
        if (obj.getParentObject() != null)
        {
            parentName = obj.getParentObject().getType() + "=" +
                         obj.getParentObject().getObjectInstanceName() + ","+
                         getHierarchicalName(obj.getParentObject());

            return parentName;
        }
        else
            return "";
    }

    protected static StringBuffer jmxEncode(StringBuffer jmxName, int attrPos)
    {
        for (int i = attrPos; i < jmxName.length(); i++)
        {
            if (jmxName.charAt(i) == ',')
            {
                jmxName.setCharAt(i, ';');
            }
            else if (jmxName.charAt(i) == ':')
            {
                jmxName.setCharAt(i, '-');
            }
            else if (jmxName.charAt(i) == '?' ||
                    jmxName.charAt(i) == '*' ||
                    jmxName.charAt(i) == '\\')
            {
                jmxName.insert(i, '\\');
                i++;
            }
            else if (jmxName.charAt(i) == '\n')
            {
                jmxName.insert(i, '\\');
                i++;
                jmxName.setCharAt(i, 'n');
            }
        }
        return jmxName;
    }
}