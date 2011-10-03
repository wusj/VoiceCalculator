package org.wolink.app.voicecalc;

import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
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
    private boolean mPlaying;
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
    	public int soundId;
    	public AssetFileDescriptor afd;
    	public boolean isLoad;
   	    public boolean isDefault;
    	
    	public Sound(int soundId, int time) {
    		this.id = -1;
    		this.time = time;
    		this.isLoad = false;
    		this.isDefault = true;
    		this.afd = null;
    		this.soundId = soundId;
    	}
    	
    	public Sound(AssetFileDescriptor afd, int time) {
    		this.id = -1;
    		this.time = time;
    		this.isLoad = false;
    		this.isDefault = false;
    		this.afd = afd;
    		this.soundId = -1;    		
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
        mPlaying = false;
    }

    /**
     * Add a new Sound to the SoundPool
     *
     * @param key - The Sound Index for Retrieval
     * @param SoundID - The Android ID for the Sound asset.
     */

    public void addSound(String key, int SoundID, int time) {
    	Sound sound = new Sound(SoundID, time);
        mSoundPoolMap.put(key, sound);
    }
    
    /**
     *
     * @param key the key we need to get the sound later
     * @param afd  the file store in the asset
     */
    public void addSound(String key, AssetFileDescriptor afd, int time) {
    	Sound sound = new Sound(afd, time);
        mSoundPoolMap.put(key, sound);
    }
   
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
    	//stopSound();
    	for(String key : keys) {
    		mSoundQueue.add(key);
    	}
    	if (!mPlaying)
    		playNextSound();
    }

    /**
     * Stop the current sound
     */
    public void stopSound() {
    	mHandler.removeCallbacks(mPlayNext);
    	mSoundQueue.clear();
        mSoundPool.stop(curStreamId);
        mPlaying = false;
    }

    public void unloadAll() {
    	stopSound();
        if (mSoundPoolMap.size() > 0) {
            for (String key : mSoundPoolMap.keySet()) {
                mSoundPool.unload(mSoundPoolMap.get(key).id);
            }
        }
        mSoundPoolMap.clear();   	
    }
    
    /**
     * Deallocates the resources and Instance of SoundManager
     */
    public void cleanup() {
    	unloadAll();
        mSoundPool.release();
        mSoundPool = null;
        _instance = null;
    }

    private void playNextSound() {
    	if (mSoundQueue.isEmpty() != true) {
	    	String key = mSoundQueue.remove(0);
	    	Sound sound = mSoundPoolMap.get(key);
	    	if (sound != null) {
	    		if (!sound.isLoad) {
	    			if (sound.isDefault) {
	    				sound.id = mSoundPool.load(mContext, sound.soundId, 1);
	    			} else {
	    				sound.id = mSoundPool.load(sound.afd, 1);
	    			}
	    			sound.isLoad = true;
	    		}
	    		
		        float streamVolume = 0.0f;
		        streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		        streamVolume /= mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		        
		        do {
		        	curStreamId = mSoundPool.play(sound.id, streamVolume, streamVolume, 1, 0, 1.0f); 
		        } while (curStreamId == 0);
		        
		        mPlaying = true;
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