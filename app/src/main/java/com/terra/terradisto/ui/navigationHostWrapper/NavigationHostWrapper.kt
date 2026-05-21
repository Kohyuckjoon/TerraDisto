package com.terra.terradisto.ui.navigationHostWrapper

import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.terra.terradisto.DistoStatusListener
import com.terra.terradisto.ui.distoconnect.ConnectDistoFragment

@Composable
fun NavigationHostWrapper(onBack: () -> Unit) {
    val activity = LocalActivity.current as? FragmentActivity
    val fragmentTag = "ConnectDistoFragment"

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    id = android.view.View.generateViewId()
                    val fragment = ConnectDistoFragment()

                    // MainActivity를 리스너로 등록
                    if (activity is DistoStatusListener) {
                        fragment.setDistoStatusListener(activity)
                    }

                    activity?.supportFragmentManager?.commit {
                        replace(id, fragment, fragmentTag)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(
                    Icons.Rounded.ArrowBackIosNew,
                    "뒤로가기",
                    tint = Color(0xFF191F28),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text("장치 연결", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191F28))
        }
    }
}
