package org.wolink.app.voicecalc;

import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;

public class SoundManager {
    private SoundPool mSoundPool;
    private HashMap<String, Sound> mSoundPoolMap;
    private AudioManager mAudioManager;
    private Context mContext;
    private Handler mHandler = new Handler();
    private Vector<String> mSoundQueue = new Vector<String>();
    private int curStreamId;
    static private SoundManager _instance;

    /**
     * Requests the instance of the Sound Manager and creates it if it does not
     * exist.
     *
     * @return Returns the single instance of the SoundManager
     */
    static synchronized public SoundManager getInstance() {
        if (_instance == null)
            _instance = new SoundManager();
        return _instance;
    }
    
    private SoundManager() {
    	// Nothing
    }
    
    private final class Sound {
    	public int id;
    	public int time;
    	public Sound(int soundId, int time) {
    		this.id = soundId;
    		this.time = time;
    	}
    }
    /**
     * Initializes the storage for the sounds
     *
     * @param theContext The Application context
     */

    public void initSounds(Context theContext) {
        mContext = theContext;
        mSoundPool = new SoundPool(1,
                AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<String, Sound>();
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Add a new Sound to the SoundPool
     *
     * @param key - The Sound Index for Retrieval
     * @param SoundID - The Android ID for the Sound asset.
     */

    public void addSound(String key, int SoundID, int time) {
    	Sound sound = new Sound(mSoundPool.load(mContext, SoundID, 1), time);
        mSoundPoolMap.put(key, sound);
    }
    
    /**
     *
     * @param key the key we need to get the sound later
     * @param afd  the file store in the asset
     */
//    public void addSound(String key, AssetFileDescriptor afd) {
//        mSoundPoolMap.put(key, mSoundPool.load(
//                afd.getFileDescriptor(),
//                afd.getStartOffset(), afd.getLength(), 1));
//    }
   
    /**
     * play the sound loaded to the SoundPool by the key we set
     * @param key  the key in the map
     */
    public void playSound(String key) {
        stopSound();
        mSoundQueue.add(key);
        playNextSound();
    }
   
	/**
	 * play the sounds have loaded in SoundPool
	 * @param keys the files key stored in the map
	 * @throws InterruptedException
	 */
    public void playSeqSounds(String keys[]) {
    	stopSound();
    	for(String key : keys) {
    		mSoundQueue.add(key);
    	}
    	playNextSound();
    }

    /**
     * Stop the current sound
     */
    public void stopSound() {
    	mHandler.removeCallbacks(mPlayNext);
    	mSoundQueue.clear();
        mSoundPool.stop(curStreamId);
    }

    /**
     * Deallocates the resources and Instance of SoundManager
     */
    public void cleanup() {
    	stopSound();
        if (mSoundPoolMap.size() > 0) {
            for (String key : mSoundPoolMap.keySet()) {
                mSoundPool.unload(mSoundPoolMap.get(key).id);
            }
        }
        mSoundPool.release();
        mSoundPool = null;
        mSoundPoolMap.clear();
        _instance = null;
    }

    private void playNextSound() {
    	if (mSoundQueue.isEmpty() != true) {
	    	String key = mSoundQueue.remove(0);
	    	Sound sound = mSoundPoolMap.get(key);
	    	if (sound != null) {
		        float streamVolume = 0.0f;
		        streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		        streamVolume /= mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		        
		        curStreamId = mSoundPool.play(sound.id, streamVolume, streamVolume, 1, 0, 1.0f); 
		        
		        mHandler.postDelayed(mPlayNext, sound.time);
	    	}
	    	else {
	    		playNextSound();
	    	}
	    }
    }
    
	private Runnable mPlayNext = new Runnable() {
		public void run() {
			mSoundPool.stop(curStreamId);
			playNextSound();			
		}
	};

}