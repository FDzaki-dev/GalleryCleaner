package com.example.gallerycleaner

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.components.GlassButton
import kotlinx.coroutines.launch

// Batch67 (Audit Gap P2 #11, stage 1/4) — title/body used to be raw String
// literals here. Moved to strings.xml, referenced by @StringRes id instead:
// this list is a top-level val evaluated outside any @Composable, so
// stringResource() (which needs LocalContext.current from composition)
// can't be called here — resolution happens later, inside
// OnboardingPageContent below, which IS a @Composable. `emoji` stays a
// plain String: it's a pictogram, not translatable text, so it's outside
// this audit item's localization/maintenance concern.
private data class OnboardingPage(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        emoji = "👉👈",
        titleRes = R.string.onboarding_page1_title,
        bodyRes = R.string.onboarding_page1_body
    ),
    OnboardingPage(
        emoji = "🗑️",
        titleRes = R.string.onboarding_page2_title,
        bodyRes = R.string.onboarding_page2_body
    ),
    OnboardingPage(
        emoji = "▦",
        titleRes = R.string.onboarding_page3_title,
        bodyRes = R.string.onboarding_page3_body
    ),
    OnboardingPage(
        emoji = "🔎",
        titleRes = R.string.onboarding_page4_title,
        bodyRes = R.string.onboarding_page4_body
    ),
    OnboardingPage(
        emoji = "🗜️",
        titleRes = R.string.onboarding_page5_title,
        bodyRes = R.string.onboarding_page5_body
    ),
    OnboardingPage(
        emoji = "📅",
        titleRes = R.string.onboarding_page6_title,
        bodyRes = R.string.onboarding_page6_body
    ),
    OnboardingPage(
        emoji = "🔍",
        titleRes = R.string.onboarding_page7_title,
        bodyRes = R.string.onboarding_page7_body
    ),
    OnboardingPage(
        emoji = "🎨",
        titleRes = R.string.onboarding_page8_title,
        bodyRes = R.string.onboarding_page8_body
    ),
    OnboardingPage(
        emoji = "🔒",
        titleRes = R.string.onboarding_page9_title,
        bodyRes = R.string.onboarding_page9_body
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == ONBOARDING_PAGES.lastIndex } }

    Scaffold(
        // Transparent (Batch22) — see matching comment in HomeScreen.kt.
        // contentColor (Batch24 fix) — see matching comment in HomeScreen.kt.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                PageIndicator(pagerState = pagerState, pageCount = ONBOARDING_PAGES.size)
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDone) {
                        Text(
                            stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GlassButton(
                        text = if (isLastPage) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                        onClick = {
                            if (isLastPage) {
                                onDone()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { page ->
            OnboardingPageContent(ONBOARDING_PAGES[page])
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(page.emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicator(pagerState: PagerState, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { i ->
            val active = pagerState.currentPage == i
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
