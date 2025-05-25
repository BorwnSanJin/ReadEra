package com.example.readera.utiles; // 或者你认为合适的任何包

import android.net.Uri;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public class UriTypeAdapter extends TypeAdapter<Uri> {

    @Override
    public void write(JsonWriter out, Uri value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString()); // 将 Uri 转换为 String 写入 JSON
        }
    }

    @Override
    public Uri read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            return Uri.parse(in.nextString()); // 从 JSON String 解析回 Uri
        }
    }
}