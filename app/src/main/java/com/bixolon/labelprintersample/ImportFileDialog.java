package com.bixolon.labelprintersample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bixolon.labelprintersample.R;

public class ImportFileDialog extends Dialog {
    private Context context;
    private List<String> item = null;
    private List<String> path = null;
    private String root = Environment.getExternalStorageDirectory().toString();
    private TextView mPath;
    private ListView lvList;
    private confirmClickListener onClicked;

    public ImportFileDialog(Context context) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_import_file);

        init();
        setListener();
        getDir(root);
    }

    private void init() {
        lvList = (ListView) findViewById(R.id.lvList);
        mPath = (TextView) findViewById(R.id.location);
    }

    private void setListener(){
        lvList.setOnItemClickListener(itemClickListener);
        ((Button)findViewById(R.id.btnConfirm)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        ((Button)findViewById(R.id.btnClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void getDir(String dirPath) {
        mPath.setText(dirPath);
        item = new ArrayList<String>();
        path = new ArrayList<String>();
        File f = new File(dirPath);
        File[] files = f.listFiles();
        if (!dirPath.equals(root)) {
            item.add(root);
            path.add(root);
            item.add("../");
            path.add(f.getParent());
        }

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if(file.getName().startsWith(".")){
                continue;
            }
            item.add(file.getName());
            path.add(file.getPath());
        }
        FileListViewAdapter fileListAdapter = new FileListViewAdapter(context, R.layout.list_row_file_list, item);
        lvList.setAdapter(fileListAdapter);
    }



    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final File file = new File(path.get(position));
            if (file.isDirectory()) {
                if (file.canRead()){
                    getDir(path.get(position));
                }else {
                    new AlertDialog.Builder(context)
                            .setTitle("[" + file.getName() + "] folder can't be read!")
                            .setPositiveButton("OK", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                }
                            }).show();
                }
            } else {
            	onClicked.onListClicked(file.getPath());
            }
        }
    };
    
    public void setConfirmClickListener(confirmClickListener clickListener){
        this.onClicked = clickListener;
    }
    
    public interface confirmClickListener{
        public void onListClicked(String filePath);
    };

    class FileListViewAdapter extends ArrayAdapter<String>{
        private Context context;
        private int resource;
        private List<String> dataList;

        public FileListViewAdapter(Context context, int resource, List<String> dataList){
            super(context, resource, dataList);
            this.context = context;
            this.resource = resource;
            this.dataList = dataList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());

            if (convertView == null){
                convertView = inflater.inflate(resource, parent, false);
            }

            final TextView tv = (TextView) convertView.findViewById(R.id.rowtext);

            tv.setText(dataList.get(position));


            if(position%2==1){
                convertView.setBackgroundColor(Color.parseColor("#F6F6F6"));
            }else{
                convertView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }

            return convertView;

        }


    }
}