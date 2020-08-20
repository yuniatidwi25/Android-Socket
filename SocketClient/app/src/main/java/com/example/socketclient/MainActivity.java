package com.example.socketclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private Button m_btn_connect, m_btn_send;
    private TextView m_txt_console;
    private ImageView img;
    private EditText m_edtxt_inputIP, m_edtxt_inputMsg, m_edtxt_inputPort;
    private Socket m_socket;
    Thread m_thread_connection, m_thread_receive, m_thread_send;
    BufferedReader m_bufferReader;
    BufferedWriter m_bufferWriter;
    Handler m_handler;
    String m_strMsg2send, m_strMsgRecceive;
    boolean m_bIsStopAllThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xmlbojectLink();
        eventLinstenerIniit();
        objectInit();

        Picasso.with(this)
                .load("https://i.imgur.com/PIMTeVd.jpg")
                .resize(250,300)
                .into(img);
    }

    void xmlbojectLink()
    {
        m_btn_connect = findViewById(R.id.ui_btn_connect);
        m_btn_send = findViewById(R.id.ui_btn_send);
        m_txt_console = findViewById(R.id.ui_txt_console);
        m_edtxt_inputIP = findViewById(R.id.ui_txt_inputIP);
        m_edtxt_inputPort = findViewById(R.id.ui_txt_inputPort);
        m_edtxt_inputMsg = findViewById(R.id.ui_txt_inputMsg);
        img = findViewById(R.id.show_image);
    }

    void eventLinstenerIniit()
    {
        m_btn_connect.setOnClickListener(onBtnClick_connect);
        m_btn_send.setOnClickListener(onBtnClick_send);
    }

    void objectInit()
    {
        m_thread_connection = null;
        m_thread_receive = null;
        m_thread_send = null;
        m_bufferReader = null;
        m_bufferWriter = null;
        m_handler = new Handler();
        m_bIsStopAllThread = false;
    }

    void connectionInit()
    {
        if ( m_socket == null && m_bufferReader == null && m_bufferWriter == null )
        {
            m_bIsStopAllThread = false;
            try
            {
                m_socket = new Socket(m_edtxt_inputIP.getText().toString(), Integer.parseInt(m_edtxt_inputPort.getText().toString()));
                m_bufferWriter = new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream()));
                m_bufferReader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));

                if ( m_socket.isConnected())
                {
                    dataStreamInit();
                }
                else
                {
                    m_handler.post(runner_releaseConnection);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


    void releaseConnection()
    {
        if ( m_socket != null && m_bufferWriter != null && m_bufferReader != null && m_thread_send != null && m_thread_receive != null && m_thread_connection != null )
        {
            m_bIsStopAllThread = true;

            m_thread_send.interrupt();
            m_thread_receive.interrupt();
            m_thread_connection.interrupt();

            try
            {
                m_bufferWriter.close();
                m_bufferReader.close();
                m_socket.close();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            m_socket = null;
            m_bufferWriter = null;
            m_bufferReader = null;
            m_thread_send = null;
            m_thread_receive = null;
            m_thread_connection = null;
        }
    }

    void dataStreamInit()
    {
        if ( m_thread_receive == null )
        {
            m_thread_receive = new Thread(runner_receive);
            m_thread_receive.start();
        }

        if ( m_thread_send == null )
        {
            m_thread_send = new Thread(runner_send);
            m_thread_send.start();
        }
    }

    Button.OnClickListener onBtnClick_connect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( m_thread_connection == null )
            {
                m_thread_connection = new Thread(runner_connect);
                m_thread_connection.start();
            }
        }
    };

    Button.OnClickListener onBtnClick_send = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            m_txt_console.append("\n" + m_edtxt_inputMsg.getText().toString() +" （From Client）" );
            m_strMsg2send = m_edtxt_inputMsg.getText().toString();
            m_edtxt_inputMsg.setText("");
        }
    };

    Runnable runner_connect = new Runnable() {
        @Override
        public void run() {
            connectionInit();
        }
    };

    Runnable runner_send = new Runnable() {
        @Override
        public void run() {
            try
            {
                while ( m_socket.isConnected() )
                {
                    if ( m_bIsStopAllThread )
                        break;

                    if ( m_strMsg2send != null )
                    {
                        m_bufferWriter.write(m_strMsg2send+"\n");
                        m_bufferWriter.flush();
                        m_strMsg2send = null;
                    }

                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    };

    Runnable runner_receive = new Runnable() {
        @Override
        public void run() {
            try
            {
                while ( m_socket.isConnected() )
                {
                    if ( m_bIsStopAllThread )
                        break;

                    m_strMsgRecceive = m_bufferReader.readLine();

                    if ( m_strMsgRecceive != null )
                    {
                        m_handler.post(runner_updateUI);
                    }
                    else
                    {
                        m_handler.post(runner_releaseConnection);
                    }

                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    };

    Runnable runner_updateUI = new Runnable() {
        @Override
        public void run() {
            if ( m_strMsgRecceive != null )
            {
                m_txt_console.append(m_strMsgRecceive);
                m_strMsgRecceive = null;
            }
        }
    };

    Runnable runner_releaseConnection = new Runnable() {
        @Override
        public void run() {
            releaseConnection();
        }
    };

}
