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
package org.apache.qpid.server;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.qpid.server.configuration.BrokerProperties;
import org.apache.qpid.server.configuration.ConfigurationEntryStore;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.configuration.store.MemoryConfigurationEntryStore;
import org.apache.qpid.server.util.StringUtil;

public class BrokerOptions
{
    /**
     * Configuration property name for the absolute path to use for the broker work directory.
     *
     * If not otherwise set, the value for this configuration property defaults to the location
     * set in the "QPID_WORK" system property if that was set, or the 'work' sub-directory of
     * the JVM working directory ("user.dir" property) for the Java process if it was not.
     */
    public static final String QPID_WORK_DIR  = "qpid.work_dir";
    /**
     * Configuration property name for the absolute path to use for the broker home directory.
     *
     * If not otherwise set, the value for this configuration property defaults to the location
     * set in the "QPID_HOME" system property if that was set, or remains unset if it was not.
     */
    public static final String QPID_HOME_DIR  = "qpid.home_dir";
    public static final String QPID_AMQP_PORT = "qpid.amqp_port";
    public static final String QPID_HTTP_PORT = "qpid.http_port";
    public static final String QPID_RMI_PORT  = "qpid.rmi_port";
    public static final String QPID_JMX_PORT  = "qpid.jmx_port";

    public static final String DEFAULT_AMQP_PORT_NUMBER = "5672";
    public static final String DEFAULT_HTTP_PORT_NUMBER = "8080";
    public static final String DEFAULT_RMI_PORT_NUMBER  = "8999";
    public static final String DEFAULT_JMX_PORT_NUMBER  = "9099";

    public static final String DEFAULT_INITIAL_CONFIG_NAME = "initial-config.json";
    public static final String DEFAULT_STORE_TYPE = "json";
    public static final String DEFAULT_CONFIG_NAME_PREFIX = "config";
    public static final String DEFAULT_LOG_CONFIG_FILE = "etc/log4j.xml";
    public static final String DEFAULT_INITIAL_CONFIG_LOCATION =
        BrokerOptions.class.getClassLoader().getResource(DEFAULT_INITIAL_CONFIG_NAME).toExternalForm();
    public static final String MANAGEMENT_MODE_USER_NAME = "mm_admin";
    private static final int MANAGEMENT_MODE_PASSWORD_LENGTH = 10;

    private static final File FALLBACK_WORK_DIR = new File(System.getProperty("user.dir"), "work");
    private static final int MAX_PORT_NUMBER = 65535;

    private String _logConfigFile;
    private Integer _logWatchFrequency = 0;

    private String _configurationStoreLocation;
    private String _configurationStoreType;

    private String _initialConfigurationLocation;

    private boolean _managementMode;
    private boolean _managementModeQuiesceVhosts;
    private int _managementModeRmiPortOverride;
    private int _managementModeJmxPortOverride;
    private int _managementModeHttpPortOverride;
    private String _managementModePassword;
    private boolean _skipLoggingConfiguration;
    private boolean _overwriteConfigurationStore;
    private Map<String, String> _configProperties = new HashMap<String,String>();

    public String getManagementModePassword()
    {
        if(_managementModePassword == null)
        {
            _managementModePassword = new StringUtil().randomAlphaNumericString(MANAGEMENT_MODE_PASSWORD_LENGTH);
        }

        return _managementModePassword;
    }

    public void setManagementModePassword(String managementModePassword)
    {
        _managementModePassword = managementModePassword;
    }

    public int getLogWatchFrequency()
    {
        return _logWatchFrequency;
    }

    /**
     * Set the frequency with which the log config file will be checked for updates.
     * @param logWatchFrequency frequency in seconds
     */
    public void setLogWatchFrequency(final int logWatchFrequency)
    {
        _logWatchFrequency = logWatchFrequency;
    }

    public boolean isManagementMode()
    {
        return _managementMode;
    }

    public void setManagementMode(boolean managementMode)
    {
        _managementMode = managementMode;
    }

    public boolean isManagementModeQuiesceVirtualHosts()
    {
        return _managementModeQuiesceVhosts;
    }

    public void setManagementModeQuiesceVirtualHosts(boolean managementModeQuiesceVhosts)
    {
        _managementModeQuiesceVhosts = managementModeQuiesceVhosts;
    }

    public int getManagementModeRmiPortOverride()
    {
        return _managementModeRmiPortOverride;
    }

    public void setManagementModeRmiPortOverride(int managementModeRmiPortOverride)
    {
        if (checkLegalPortNumber(managementModeRmiPortOverride))
        {
            throw new IllegalConfigurationException("Invalid rmi port is specified: " + managementModeRmiPortOverride);
        }
        _managementModeRmiPortOverride = managementModeRmiPortOverride;
    }

    public int getManagementModeJmxPortOverride()
    {
        return _managementModeJmxPortOverride;
    }

    public void setManagementModeJmxPortOverride(int managementModeJmxPortOverride)
    {
        if (checkLegalPortNumber(managementModeJmxPortOverride))
        {
            throw new IllegalConfigurationException("Invalid jmx port is specified: " + managementModeJmxPortOverride);
        }
        _managementModeJmxPortOverride = managementModeJmxPortOverride;
    }

    public int getManagementModeHttpPortOverride()
    {
        return _managementModeHttpPortOverride;
    }

    public void setManagementModeHttpPortOverride(int managementModeHttpPortOverride)
    {
        if (checkLegalPortNumber(managementModeHttpPortOverride))
        {
            throw new IllegalConfigurationException("Invalid http port is specified: " + managementModeHttpPortOverride);
        }
        _managementModeHttpPortOverride = managementModeHttpPortOverride;
    }

    /**
     * Get the broker configuration store type.
     *
     * @return the previously set store type, or if none was set the default: {@value #DEFAULT_STORE_TYPE}
     */
    public String getConfigurationStoreType()
    {
        if(_configurationStoreType == null)
        {
            return  DEFAULT_STORE_TYPE;
        }

        return _configurationStoreType;
    }

    /**
     * Set the broker configuration store type.
     *
     * Passing null clears previously set values and returns to the default.
     */
    public void setConfigurationStoreType(String configurationStoreType)
    {
        _configurationStoreType = configurationStoreType;
    }

    /**
     * Get the broker configuration store location.
     *
     * Defaults to {@value #DEFAULT_CONFIG_NAME_PREFIX}.{@literal <store type>} (see {@link BrokerOptions#getConfigurationStoreType()})
     * within the broker work directory (gathered via config property {@link #QPID_WORK_DIR}).
     *
     * @return the previously set configuration store location, or the default location if none was set.
     */
    public String getConfigurationStoreLocation()
    {
        if(_configurationStoreLocation == null)
        {
            String workDir = getWorkDir();
            String storeType = getConfigurationStoreType();

            return new File(workDir, DEFAULT_CONFIG_NAME_PREFIX + "." + storeType).getAbsolutePath();
        }

        return _configurationStoreLocation;
    }

    /**
     * Set the absolute path to use for the broker configuration store.
     *
     * Passing null clears any previously set value and returns to the default.
     */
    public void setConfigurationStoreLocation(String configurationStore)
    {
        _configurationStoreLocation = configurationStore;
    }

    /**
     * Returns whether the existing broker configuration store should be overwritten with the current
     * initial configuration file (see {@link BrokerOptions#getInitialConfigurationLocation()}).
     */
    public boolean isOverwriteConfigurationStore()
    {
        return _overwriteConfigurationStore;
    }

    /**
     * Sets whether the existing broker configuration store should be overwritten with the current
     * initial configuration file (see {@link BrokerOptions#getInitialConfigurationLocation()}).
     */
    public void setOverwriteConfigurationStore(boolean overwrite)
    {
        _overwriteConfigurationStore = overwrite;
    }

    /**
     * Get the broker initial JSON configuration location.
     *
     * Defaults to an internal configuration file within the broker jar.
     *
     * @return the previously set configuration location, or the default location if none was set.
     */
    public String getInitialConfigurationLocation()
    {
        if(_initialConfigurationLocation == null)
        {
            return DEFAULT_INITIAL_CONFIG_LOCATION;
        }

        return _initialConfigurationLocation;
    }

    /**
     * Set the absolute path or URL to use for the initial JSON configuration, which is loaded with the
     * {@link MemoryConfigurationEntryStore} in order to initialise any new {@link ConfigurationEntryStore} for the broker.
     *
     * Passing null clears any previously set value and returns to the default.
     */
    public void setInitialConfigurationLocation(String initialConfigurationLocation)
    {
        _initialConfigurationLocation = initialConfigurationLocation;
    }

    public boolean isSkipLoggingConfiguration()
    {
        return _skipLoggingConfiguration;
    }

    public void setSkipLoggingConfiguration(boolean skipLoggingConfiguration)
    {
        _skipLoggingConfiguration = skipLoggingConfiguration;
    }

    /**
     * Sets the named configuration property to the given value.
     *
     * Passing a null value causes removal of a previous value, and restores any default there may have been.
     */
    public void setConfigProperty(String name, String value)
    {
        if(value == null)
        {
            _configProperties.remove(name);
        }
        else
        {
            _configProperties.put(name, value);
        }
    }

    /**
     * Get an un-editable copy of the configuration properties, representing
     * the user-configured values as well as any defaults for properties
     * not otherwise configured.
     *
     * Subsequent property changes are not reflected in this map.
     */
    public Map<String,String> getConfigProperties()
    {
        ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<String,String>();
        properties.putAll(_configProperties);

        properties.putIfAbsent(QPID_AMQP_PORT, String.valueOf(DEFAULT_AMQP_PORT_NUMBER));
        properties.putIfAbsent(QPID_HTTP_PORT, String.valueOf(DEFAULT_HTTP_PORT_NUMBER));
        properties.putIfAbsent(QPID_RMI_PORT, String.valueOf(DEFAULT_RMI_PORT_NUMBER));
        properties.putIfAbsent(QPID_JMX_PORT, String.valueOf(DEFAULT_JMX_PORT_NUMBER));
        properties.putIfAbsent(QPID_WORK_DIR, getWorkDir());

        String homeDir = getHomeDir();
        if(homeDir != null)
        {
            properties.putIfAbsent(QPID_HOME_DIR, homeDir);
        }

        return Collections.unmodifiableMap(properties);
    }

    /**
     * Get the broker logging configuration file location.
     *
     * If not previously explicitly set, defaults to {@value #DEFAULT_LOG_CONFIG_FILE} within the broker
     * home directory if configured (gathered via config property {@link #QPID_HOME_DIR}) or the current
     * JVM working directory if not.
     *
     * @return the previously set logging configuration file location, or the default location if none was set.
     */
    public String getLogConfigFileLocation()
    {
        if(_logConfigFile == null)
        {
            String homeDir = getHomeDir();

            return new File(homeDir, DEFAULT_LOG_CONFIG_FILE).getAbsolutePath();
        }

        return _logConfigFile;
    }

    public void setLogConfigFileLocation(final String logConfigFile)
    {
        _logConfigFile = logConfigFile;
    }

    private String getWorkDir()
    {
        if(!_configProperties.containsKey(QPID_WORK_DIR))
        {
            String qpidWork = System.getProperty(BrokerProperties.PROPERTY_QPID_WORK);
            if (qpidWork == null)
            {
                return FALLBACK_WORK_DIR.getAbsolutePath();
            }

            return qpidWork;
        }

        return _configProperties.get(QPID_WORK_DIR);
    }

    private String getHomeDir()
    {
        if(!_configProperties.containsKey(QPID_HOME_DIR))
        {
            return System.getProperty(BrokerProperties.PROPERTY_QPID_HOME);
        }

        return _configProperties.get(QPID_HOME_DIR);
    }

    private boolean checkLegalPortNumber(int portNumber)
    {
        return portNumber < 1 || portNumber > MAX_PORT_NUMBER;
    }


}
