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
package org.cryptoworkshop.ximix.node.crypto.key;

import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERUTF8String;
import org.cryptoworkshop.ximix.client.connection.ServiceConnectionException;
import org.cryptoworkshop.ximix.client.connection.ServicesConnection;
import org.cryptoworkshop.ximix.common.asn1.message.AlgorithmServiceMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CapabilityMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CommandMessage;
import org.cryptoworkshop.ximix.common.asn1.message.KeyPairGenerateMessage;
import org.cryptoworkshop.ximix.common.asn1.message.Message;
import org.cryptoworkshop.ximix.common.asn1.message.MessageReply;
import org.cryptoworkshop.ximix.common.asn1.message.MessageType;
import org.cryptoworkshop.ximix.common.asn1.message.NamedKeyGenParams;
import org.cryptoworkshop.ximix.common.asn1.message.StoreMessage;
import org.cryptoworkshop.ximix.common.crypto.Algorithm;
import org.cryptoworkshop.ximix.common.util.EventNotifier;
import org.cryptoworkshop.ximix.node.crypto.key.message.BLSCommittedSecretShareMessage;
import org.cryptoworkshop.ximix.node.service.NodeContext;

/**
 * A generator for BLS keys.
 */
public class BLSKeyPairGenerator
{
    public static enum Type
        implements MessageType
    {
        GENERATE,   // must always be first.
        STORE
    }

    private final NodeContext nodeContext;

    /**
     * Base constructor.
     *
     * @param nodeContext the node context this generator is associated with.
     */
    public BLSKeyPairGenerator(NodeContext nodeContext)
    {
        this.nodeContext = nodeContext;
    }

    public CapabilityMessage getCapability()
    {
        return new CapabilityMessage(CapabilityMessage.Type.KEY_GENERATION, new ASN1Encodable[0]); // TODO:
    }

    public MessageReply handle(KeyPairGenerateMessage message)
    {
        // TODO: sort out the reply messages
        try
        {
            switch (((Type)message.getType()))
            {
            case GENERATE:
                final NamedKeyGenParams ecKeyGenParams = (NamedKeyGenParams)NamedKeyGenParams.getInstance(message.getPayload());
                final List<String> involvedPeers = ecKeyGenParams.getNodesToUse();

                if (involvedPeers.contains(nodeContext.getName()))
                {
                    BLSNewDKGGenerator generator = (BLSNewDKGGenerator)nodeContext.getKeyPairGenerator(ecKeyGenParams.getAlgorithm());

                    BLSCommittedSecretShareMessage[] messages = generator.generateThresholdKey(ecKeyGenParams.getKeyID(), ecKeyGenParams);

                    nodeContext.execute(new SendShareTask(generator, ecKeyGenParams.getAlgorithm(), ecKeyGenParams.getKeyID(), involvedPeers, messages));
                }

                return new MessageReply(MessageReply.Type.OKAY);
            case STORE:
                StoreMessage sssMessage = StoreMessage.getInstance(message.getPayload());

                // we may not have been asked to generate our share yet, if this is the case we need to queue up our share requests
                // till we can validate them.
                BLSNewDKGGenerator generator = (BLSNewDKGGenerator)nodeContext.getKeyPairGenerator(message.getAlgorithm());

                nodeContext.execute(new StoreShareTask(generator, sssMessage.getID(), sssMessage.getSecretShareMessage()));

                return new MessageReply(MessageReply.Type.OKAY);
            default:
                nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, "Unknown command in NodeKeyGenerationService.");

                return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String("Unknown command in NodeKeyGenerationService."));
            }
        }
        catch (Exception e)
        {
            nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, "NodeKeyGenerationService failure: " + e.getMessage(), e);

            return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String("NodeKeyGenerationService failure: " + e.getMessage()));
        }
    }

    public boolean isAbleToHandle(Message message)
    {
        Enum type = message.getType();

        return type == CommandMessage.Type.GENERATE_KEY_PAIR
            || type == CommandMessage.Type.STORE_SHARE;
    }

    private class SendShareTask
        implements Runnable
    {
        private final BLSNewDKGGenerator generator;
        private final String keyID;
        private final List<String> peers;
        private final BLSCommittedSecretShareMessage[] messages;
        private final Algorithm algorithm;

        SendShareTask(BLSNewDKGGenerator generator, Algorithm algorithm, String keyID, List<String> peers, BLSCommittedSecretShareMessage[] messages)
        {
            this.generator = generator;
            this.algorithm = algorithm;
            this.keyID = keyID;
            this.peers = peers;
            this.messages = messages;
        }

        public void run()
        {
            int index = 0;

            for (final String name : peers)
            {
                if (name.equals(nodeContext.getName()))
                {
                    generator.storeThresholdKeyShare(keyID, messages[index++]);
                }
                else
                {
                    nodeContext.execute(new SendShareToNodeTask(name, keyID, algorithm, messages[index++]));
                }
            }
        }
    }

    private class SendShareToNodeTask
        implements Runnable
    {
        private final String name;
        private final String keyID;
        private final Algorithm algorithm;
        private final BLSCommittedSecretShareMessage shareMessage;

        SendShareToNodeTask(String name, String keyID, Algorithm algorithm, BLSCommittedSecretShareMessage shareMessage)
        {
            this.name = name;
            this.keyID = keyID;
            this.algorithm = algorithm;
            this.shareMessage = shareMessage;
        }

        public void run()
        {
            try
            {
                ServicesConnection connection = nodeContext.getPeerMap().get(name);
                if (connection != null)
                {
                    MessageReply rep = connection.sendMessage(CommandMessage.Type.GENERATE_KEY_PAIR, new AlgorithmServiceMessage(algorithm, new KeyPairGenerateMessage(algorithm, Type.STORE, new StoreMessage(keyID, shareMessage))));
                    if (rep.getType() != MessageReply.Type.OKAY)
                    {
                        nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, "Error in SendShare: " + rep.interpretPayloadAsError());
                    }
                }
                else
                {
                    nodeContext.getEventNotifier().notify(EventNotifier.Level.WARN, "Node " + name + " not connected, retrying");
                    try
                    {
                        Thread.sleep(2000);    // TODO: configurable?
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                    nodeContext.execute(SendShareToNodeTask.this);
                }
            }
            catch (ServiceConnectionException e)
            {
                nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, "Exception in SendShareToNodeTask: " + e.getMessage(), e);
            }
        }
    }

    private class StoreShareTask
        implements Runnable
    {
        private final BLSNewDKGGenerator generator;
        private final String keyID;
        private final ASN1Encodable message;

        StoreShareTask(BLSNewDKGGenerator generator, String keyID, ASN1Encodable message)
        {
            this.generator = generator;
            this.keyID = keyID;
            this.message = message;
        }

        @Override
        public void run()
        {
            if (nodeContext.hasPrivateKey(keyID))
            {
                generator.storeThresholdKeyShare(keyID, BLSCommittedSecretShareMessage.getInstance(generator.getParameters(keyID), message));
            }
            else
            {
                nodeContext.getEventNotifier().notify(EventNotifier.Level.WARN, "Still waiting for generate message for key " + keyID);
                try
                {
                    Thread.sleep(1000);   // TODO: configurable?
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                nodeContext.execute(StoreShareTask.this);
            }
        }
    }
}
