/**
 * This file is part of aion-emu <aion-emu.com>.
 *
 *  aion-emu is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-emu is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openaion.packetsamurai.logreaders;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import javolution.util.FastList;

import org.openaion.packetsamurai.PacketHandler;
import org.openaion.packetsamurai.PacketSamurai;
import org.openaion.packetsamurai.gui.Main;
import org.openaion.packetsamurai.protocol.protocoltree.PacketFamilly.packetDirection;
import org.openaion.packetsamurai.session.DataPacket;
import org.openaion.packetsamurai.session.Session;


/**
 * @author kami
 * 
 */
public class PSLReader extends AbstractLogReader
{
	private RandomAccessFile logFile;
	private Session session;
	private int totalPacketCount;
	private FastList<DataPacket> packets;
	private int headerSize;
	private final ByteBuffer bb;
	private final FileChannel fc;

	protected PacketHandler packetHandler;

	public PSLReader(String filename) throws IOException
	{
		super(filename);
		logFile = new RandomAccessFile(filename, "r");
		bb = ByteBuffer.allocate((int)logFile.length());
		fc = logFile.getChannel();
		fc.read(bb);
		packets = new FastList<DataPacket>();
	}

	@Override
	protected String getAditionalName()
	{
		return "psl";
	}

	@Override
	public boolean parseHeader()
	{
		try
		{
			bb.flip();
			headerSize = bb.remaining();
			bb.order(ByteOrder.LITTLE_ENDIAN);
			bb.get(); // logVersion
			totalPacketCount = bb.getInt();
			bb.get();
			bb.getShort();
			short port = bb.getShort();
			Inet4Address cilentIp = (Inet4Address) Inet4Address.getByAddress(readBytes(bb, 4));
			Inet4Address serverIp = (Inet4Address) Inet4Address.getByAddress(readBytes(bb, 4));
			readString(bb); // protocolName
			String sessionComments = readString(bb);
			String serverType = readString(bb);
			long analyserBitSet = bb.getLong();
			long sessionID = bb.getLong();
			boolean isDecrypted = (bb.get() & 0xFF) == 0x00 ? true : false;
			headerSize -= bb.remaining();
			session = new Session(sessionID, AbstractLogReader.getLogProtocolByPort(port), "live", !isDecrypted);
			session.setAnalyserBitSet(analyserBitSet);
			session.setClientIp(cilentIp);
			session.setComments(sessionComments);
			session.setServerIp(serverIp);
			session.setServerType(serverType);
			session.setShown(true);
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean parsePackets() throws IOException
	{
		short packetSize;
		long timestamp;
		byte[] data;
		packetDirection pd;
		DataPacket packet;

		bb.rewind();
		bb.position(headerSize);
		for (int i = 0; i < totalPacketCount; i++) {
			pd = (bb.get() & 0xFF) == 0x01 ? packetDirection.serverPacket : packetDirection.clientPacket;
			packetSize = bb.getShort();
			timestamp = bb.getLong();
			data = readBytes(bb, packetSize - 2);
			
			/*
			//testing dropping new 2.7, 2x bytes between opcode and checksum, unknown what they are for. 0x66 SM_KEY is slightly different.
			//This effectively drops the extra 2 bytes but SM_KEY needs to be adjusted so correct values and key are read.
			byte[] newData = new byte[packetSize -4];
			System.arraycopy(data, 0, newData, 0, 1);
			System.arraycopy(data, 3, newData, 1, packetSize -5);
			*/

			packet = new DataPacket(data, pd, timestamp, session.getProtocol(), true);
			packets.add(packet);
		}
		session.setPackets(packets);
		((Main) PacketSamurai.getUserInterface()).showSession(session, true);
		return true;
	}

	@Override
	protected void closeFile() throws IOException
	{
		bb.clear();
		fc.close();
		logFile.close();
	}

	@Override
	protected String getFileExtension()
	{
		return "psl";
	}

	@Override
	public boolean supportsHeaderReading()
	{
		return false;
	}

	private String readString(ByteBuffer buf)
	{
		StringBuffer sb = new StringBuffer();
		char ch;
		try
		{
			while ((ch = buf.getChar()) != '\000')
				sb.append(ch);
		}
		catch (Exception e)
		{}
		return sb.toString();
	}

	public final byte[] readBytes(ByteBuffer buf, int length)
	{
		byte[] result = new byte[length];
		try
		{
			buf.get(result);
		}
		catch (Exception e)
		{}
		return result;
	}
}
