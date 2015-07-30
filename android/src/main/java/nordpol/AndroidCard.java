package nordpol.android;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import android.os.Build;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import nordpol.IsoCard;
import nordpol.OnCardErrorListener;

public class AndroidCard implements IsoCard {
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int SAMSUNG_S5_MINI_MAX = 253;

    private IsoDep card;
    private List<OnCardErrorListener> errorListeners =
        new CopyOnWriteArrayList<OnCardErrorListener>();

    private AndroidCard(IsoDep card) {
        this.card = card;
    }

    /**
     * Model names&numbers: 
     * https://support.google.com/googleplay/android-developer/answer/6154891?hl=en 
     */
    private static boolean isSamsungS5() {
    	return (Build.MANUFACTURER.equals("Samsung") 
                		&& (((Build.DEVICE.startsWith("k") || Build.DEVICE.startsWith("l")) && (Build.MODEL.contains("SM-G")))
                				|| (Build.DEVICE.contains("SCL23"))));
    }

    public static AndroidCard get(Tag tag) throws IOException {
        IsoDep card = IsoDep.get(tag);

        if(card != null) {
            if (isSamsungS5()) {
            	/* Workaround for the Samsung Galaxy S5 (since the
            	 * first connection always hangs on transceive).
            	 */
            	card.connect();
            	card.close();
            }
            return new AndroidCard(card);
        } else {
            return null;
        }
    }

    private void notifyListeners(IOException exception) {
        for(OnCardErrorListener listener: errorListeners) {
            listener.error(this, exception);
        }
    }

    @Override
    public void addOnCardErrorListener(OnCardErrorListener listener) {
        errorListeners.add(listener);
    }

    @Override
    public void removeOnCardErrorListener(OnCardErrorListener listener) {
        errorListeners.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return card.isConnected();
    }

    @Override
    public void connect() throws IOException {
        try {
            card.connect();
            card.setTimeout(DEFAULT_TIMEOUT);
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    @Override
    public int getMaxTransceiveLength() throws IOException {
        if (isSamsungS5()) {
        	return Math.min(card.getMaxTransceiveLength(), SAMSUNG_S5_MINI_MAX);	
        }
        return card.getMaxTransceiveLength();
    }

    @Override
    public int getTimeout() {
        return card.getTimeout();
    }

    @Override
    public void setTimeout(int timeout) {
        card.setTimeout(timeout);
    }

    @Override
    public void close() throws IOException {
        try {
            card.close();
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    @Override
    public byte[] transceive(byte [] command) throws IOException {
        try {
            return card.transceive(command);
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    @Override
    public List<byte[]> transceive(List<byte[]> commands) throws IOException {
        try {
            ArrayList<byte[]> responses = new ArrayList<byte[]>();
            for(byte[] command: commands) {
                responses.add(card.transceive(command));
            }
            return responses;
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    public Tag getTag() {
        return card.getTag();
    }
    
    public byte[] getHistoricalBytes() {
        return card.getHistoricalBytes();
    }
}
