package com.example;

import android.app.ProgressDialog;
import android.content.Context;

public class SAProgress {
	
	private ProgressDialog mDialog;
	
	private static final SAProgress progress = new SAProgress();

    public static SAProgress getInstance()
    {
        return progress;
    }
    
	public ProgressDialog show(final Context context, final String msg)
    {
        this.mDialog = new ProgressDialog(context);
        this.mDialog.setMessage(msg);
        this.mDialog.setCancelable(false);
        this.mDialog.show();

        return this.mDialog;
    }
	
	public void hide()
    {
        if (null != this.mDialog)
        {
            this.mDialog.hide();
        }
    }
}

