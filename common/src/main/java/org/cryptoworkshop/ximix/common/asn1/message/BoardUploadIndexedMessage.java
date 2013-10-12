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
package org.cryptoworkshop.ximix.common.asn1.message;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Carrier message for the upload of single message at a particular index.
 */
public class BoardUploadIndexedMessage
    extends ASN1Object
{
    private final String boardName;
    private final int index;
    private final byte[] data;

    /**
     * Base constructor.
     *
     * @param boardName the name of board the message is destined for.
     * @param indexNumbder the index number the message is to reside out.
     * @param data the message data.
     */
    public BoardUploadIndexedMessage(String boardName, int indexNumbder, byte[] data)
    {
        this.boardName = boardName;
        this.index = indexNumbder;
        this.data = data.clone();
    }

    private BoardUploadIndexedMessage(ASN1Sequence seq)
    {
        this.boardName = DERUTF8String.getInstance(seq.getObjectAt(0)).getString();
        this.index = ASN1Integer.getInstance(seq.getObjectAt(1)).getValue().intValue();
        this.data = ASN1OctetString.getInstance(seq.getObjectAt(2)).getOctets();
    }

    public static final BoardUploadIndexedMessage getInstance(Object o)
    {
        if (o instanceof BoardUploadIndexedMessage)
        {
            return (BoardUploadIndexedMessage)o;
        }
        else if (o != null)
        {
            return new BoardUploadIndexedMessage(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERUTF8String(boardName));
        v.add(new ASN1Integer(index));
        v.add(new DEROctetString(data));

        return new DERSequence(v);
    }

    public String getBoardName()
    {
        return boardName;
    }

    public byte[] getData()
    {
        return data;
    }

    public int getIndex()
    {
        return index;
    }
}
