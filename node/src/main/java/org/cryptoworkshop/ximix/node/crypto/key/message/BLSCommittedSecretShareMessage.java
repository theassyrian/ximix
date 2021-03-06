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
package org.cryptoworkshop.ximix.node.crypto.key.message;

import java.math.BigInteger;

import it.unisa.dia.gas.crypto.jpbc.signature.bls01.params.BLS01Parameters;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

/**
 * Carrier class for a BLS secret share with a commitment.
 */
public class BLSCommittedSecretShareMessage
    extends ASN1Object
{
    private final int        index;
    private final BigInteger value;
    private final BigInteger witness;
    private final Element pK;
    private final Element[] commitmentFactors;

    /**
     * Base constructor.
     *
     * @param index sequence number of the share.
     * @param value share value.
     * @param witness witness value associated with share.
     * @param commitmentFactors commitment factors associated with share.
     * @param pK the public value associated with the secret.
     */
    public BLSCommittedSecretShareMessage(int index, BigInteger value, BigInteger witness, Element[] commitmentFactors, Element pK)
    {
        this.index = index;
        this.value = value;
        this.witness = witness;
        this.commitmentFactors = commitmentFactors;
        this.pK = pK;
    }

    private BLSCommittedSecretShareMessage(BLS01Parameters blsParameters, ASN1Sequence seq)
    {
        this.index = ASN1Integer.getInstance(seq.getObjectAt(0)).getValue().intValue();
        this.value = ASN1Integer.getInstance(seq.getObjectAt(1)).getValue();
        this.witness = ASN1Integer.getInstance(seq.getObjectAt(2)).getValue();

        ASN1Sequence s = ASN1Sequence.getInstance(seq.getObjectAt(3));

        this.commitmentFactors = new Element[s.size()];

        for (int i = 0; i != commitmentFactors.length; i++)
        {
            commitmentFactors[i] = blsParameters.getG().duplicate();
            commitmentFactors[i].setFromBytes(DEROctetString.getInstance(s.getObjectAt(i)).getOctets());
        }

        Pairing pairing = PairingFactory.getPairing(blsParameters.getCurveParameters());
        this.pK = pairing.getG2().newElement();
        this.pK.setFromBytes(DEROctetString.getInstance(seq.getObjectAt(4)).getOctets());
    }

    public static final BLSCommittedSecretShareMessage getInstance(BLS01Parameters blsParameters, Object o)
    {
        if (o instanceof BLSCommittedSecretShareMessage)
        {
            return (BLSCommittedSecretShareMessage)o;
        }
        else if (o != null)
        {
            return new BLSCommittedSecretShareMessage(blsParameters, ASN1Sequence.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(index));
        v.add(new ASN1Integer(value));
        v.add(new ASN1Integer(witness));

        ASN1EncodableVector factV = new ASN1EncodableVector();
        for (int i = 0; i != commitmentFactors.length; i++)
        {
            factV.add(new DEROctetString(commitmentFactors[i].toBytes()));
        }

        v.add(new DERSequence(factV));
        v.add(new DEROctetString(pK.toBytes()));

        return new DERSequence(v);
    }

    public int getIndex()
    {
        return index;
    }

    public BigInteger getValue()
    {
        return value;
    }

    public BigInteger getWitness()
    {
        return witness;
    }

    public Element getPk()
    {
        return pK;
    }

    public Element[] getCommitmentFactors()
    {
        return commitmentFactors;
    }
}
