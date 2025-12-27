package com.creategoodthings.markdownKeyboard.ui

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MainPage(modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        val context = LocalContext.current
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        ) {
            Text("Enable Keyboard")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        ) {
            Text("Activate Keyboard")
        }

        val (text, setTextValue) = remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            modifier = Modifier
                .fillMaxWidth(),
            onValueChange = setTextValue,
            shape = RoundedCornerShape(24.dp),
            minLines = 8
        )
    }
}