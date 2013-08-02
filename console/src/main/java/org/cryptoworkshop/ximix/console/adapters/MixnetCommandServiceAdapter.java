/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.console.adapters;

import org.cryptoworkshop.ximix.common.config.Config;
import org.cryptoworkshop.ximix.common.console.annotations.CommandParam;
import org.cryptoworkshop.ximix.common.console.annotations.ConsoleCommand;
import org.cryptoworkshop.ximix.common.message.NodeStatusMessage;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;
import org.cryptoworkshop.ximix.console.config.AdapterConfig;
import org.cryptoworkshop.ximix.console.config.ConsoleConfig;
import org.cryptoworkshop.ximix.console.handlers.messages.StandardMessage;
import org.cryptoworkshop.ximix.console.model.AdapterInfo;
import org.cryptoworkshop.ximix.mixnet.ShuffleOptions;
import org.cryptoworkshop.ximix.mixnet.admin.CommandService;
import org.cryptoworkshop.ximix.monitor.NodeHealthMonitor;
import org.cryptoworkshop.ximix.registrar.RegistrarServiceException;
import org.cryptoworkshop.ximix.registrar.XimixRegistrar;
import org.cryptoworkshop.ximix.registrar.XimixRegistrarFactory;

import java.io.File;
import java.util.*;

/**
 * An adapter for the Mixnet commands service.
 */
public class MixnetCommandServiceAdapter
    extends BaseNodeAdapter
{

    protected File configFile = null;
    protected XimixRegistrar registrar = null;
    protected CommandService commandService = null;
    protected Class commandType = CommandService.class;
    protected Config config = null;
    protected List<XimixRegistrarFactory.NodeConfig> configuredNodes = null;
    protected Map<String, XimixRegistrarFactory.NodeConfig> nameToConfig = null;

    public MixnetCommandServiceAdapter()
    {
        super();
    }


//    @Override
//    public void init(String name, Config config)
//            throws Exception
//    {
//        this.id = name;
//        this.config = config;
//        // this.configFile = new File(config.getProperty("config.file"));
//        commandList = new ArrayList<>();
//        findCommands(this);
//    }

    @Override
    public AdapterInfo getInfo()
    {
        AdapterInfo info = new AdapterInfo();
        info.setId(id);
        info.setName(name);
        info.setDescription(description);
        return info;
    }

    @Override
    public void init(ConsoleConfig consoleConfig, AdapterConfig config)
        throws Exception
    {
        commandList = new ArrayList<>();
        super.init(consoleConfig, config);

        String f = config.get("mixnet-file").toString();
        if (f.isEmpty())
        {
            f = System.getProperty("mixnet-file");
        }

        if (f == null || f.isEmpty())
        {
            throw new RuntimeException("Mixnet file not specified.");
        }

        configFile = new File(f);
        findCommands(this);
    }

    @Override
    public void open()
        throws Exception
    {
        try
        {
            registrar = XimixRegistrarFactory.createAdminServiceRegistrar(configFile);
            configuredNodes = registrar.getConfiguredNodeNames();
            nameToConfig = new HashMap<>();
            for (XimixRegistrarFactory.NodeConfig nc : configuredNodes)
            {
                nameToConfig.put(nc.getName(), nc);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex); // TODO handle this better.
        }

        opened = true;
    }

    @Override
    public void close()
        throws Exception
    {
        // TODO close it.
    }

    @ConsoleCommand(name = "Do Shuffle & Move")
    public StandardMessage doShuffleAndMove(
        @CommandParam(name = "Board Name")
        String boardName,
        @CommandParam(name = "Transform Name")
        String transformName,
        @CommandParam(name = "Key id")
        String keyID,
        @CommandParam(name = "Nodes")
        String... nodes)
        throws ServiceConnectionException
    {

        //TODO add sensitisation.

        ShuffleOptions.Builder builder = new ShuffleOptions.Builder(transformName);
        builder.setKeyID(keyID);
        commandService.doShuffleAndMove(boardName, builder.build(), nodes);
        return new StandardMessage(true, "It worked..");
    }

    @Override
    public List<XimixRegistrarFactory.NodeConfig> getConfiguredNodes()
    {
        return configuredNodes;
    }

    @Override
    public List<XimixRegistrarFactory.NodeConfig> getConnectedNodes()
    {
        ArrayList<XimixRegistrarFactory.NodeConfig> out = new ArrayList<>();
        try
        {
            NodeHealthMonitor nhm = registrar.connect(NodeHealthMonitor.class);
            Set<String> names = nhm.getConnectedNodeNames();

            for (String n : names)
            {
                out.add(nameToConfig.get(n));
            }

        }
        catch (RegistrarServiceException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return out;
    }

    @Override
    public NodeStatusMessage getNodeDetails(String name)
    {
        NodeStatusMessage details = null;
        try
        {
            NodeHealthMonitor nhm = registrar.connect(NodeHealthMonitor.class);
            details = nhm.getFullInfo(name);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return details;
    }

    @Override
    public List<NodeStatusMessage> getNodeDetails()
    {
        List<NodeStatusMessage> details = null;
        try
        {
            NodeHealthMonitor nhm = registrar.connect(NodeHealthMonitor.class);
            details = nhm.getFullInfo();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return details;

    }

    @Override
    public NodeStatusMessage getNodeStatistics(String node)
    {
        NodeStatusMessage details = null;
        try
        {
            NodeHealthMonitor nhm = registrar.connect(NodeHealthMonitor.class);
            details = nhm.getStatistics(node);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return details;
    }

}
