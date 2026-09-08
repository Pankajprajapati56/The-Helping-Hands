package com.helpinghands.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Member(val id:String, val name:String, val mobile:String, val status:String="Active")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HelpingHandsApp() }
    }
}

@Composable
fun HelpingHandsApp() {
    var loggedIn by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    MaterialTheme {
        if (!loggedIn) LoginScreen { admin ->
            isAdmin = admin
            loggedIn = true
        } else DashboardScreen(isAdmin) { loggedIn = false }
    }
}

@Composable
fun LoginScreen(onLogin:(Boolean)->Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("THE HELPING HANDS", style=MaterialTheme.typography.headlineMedium)
        Text("Management System", style=MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(user, {user=it}, label={Text("User ID")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pass, {pass=it}, label={Text("Password")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick={
            // Demo login for Phase 3 UI. Production auth will be connected to database/backend.
            if (user == "admin" && pass == "admin123") onLogin(true)
            else if (user.isNotBlank() && pass.isNotBlank()) onLogin(false)
            else error = true
        }, modifier=Modifier.fillMaxWidth()) { Text("LOGIN") }
        if (error) Text("User ID और Password भरें", color=MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("Demo Admin: admin / admin123", style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DashboardScreen(isAdmin:Boolean, logout:()->Unit) {
    var page by remember { mutableStateOf("home") }
    val members = listOf(
Member("=IF(C2=\"\",\"\",\"THH\"&TEXT(A2,\"000\"))","","","Active"),
Member("=IF(C3=\"\",\"\",\"THH\"&TEXT(A3,\"000\"))","","","Active"),
Member("=IF(C4=\"\",\"\",\"THH\"&TEXT(A4,\"000\"))","","","Active"),
Member("=IF(C5=\"\",\"\",\"THH\"&TEXT(A5,\"000\"))","","","Active"),
Member("=IF(C6=\"\",\"\",\"THH\"&TEXT(A6,\"000\"))","","","Active"),
Member("=IF(C7=\"\",\"\",\"THH\"&TEXT(A7,\"000\"))","","","Active"),
Member("=IF(C8=\"\",\"\",\"THH\"&TEXT(A8,\"000\"))","","","Active"),
Member("=IF(C9=\"\",\"\",\"THH\"&TEXT(A9,\"000\"))","","","Active"),
Member("=IF(C10=\"\",\"\",\"THH\"&TEXT(A10,\"000\"))","","","Active"),
Member("=IF(C11=\"\",\"\",\"THH\"&TEXT(A11,\"000\"))","","","Active"),
Member("=IF(C12=\"\",\"\",\"THH\"&TEXT(A12,\"000\"))","","","Active"),
Member("=IF(C13=\"\",\"\",\"THH\"&TEXT(A13,\"000\"))","","","Active"),
Member("=IF(C14=\"\",\"\",\"THH\"&TEXT(A14,\"000\"))","","","Active"),
Member("=IF(C15=\"\",\"\",\"THH\"&TEXT(A15,\"000\"))","","","Active"),
Member("=IF(C16=\"\",\"\",\"THH\"&TEXT(A16,\"000\"))","","","Active"),
Member("=IF(C17=\"\",\"\",\"THH\"&TEXT(A17,\"000\"))","","","Active"),
Member("=IF(C18=\"\",\"\",\"THH\"&TEXT(A18,\"000\"))","","","Active"),
Member("=IF(C19=\"\",\"\",\"THH\"&TEXT(A19,\"000\"))","","","Active"),
Member("=IF(C20=\"\",\"\",\"THH\"&TEXT(A20,\"000\"))","","","Active"),
Member("=IF(C21=\"\",\"\",\"THH\"&TEXT(A21,\"000\"))","","","Active"),
Member("=IF(C22=\"\",\"\",\"THH\"&TEXT(A22,\"000\"))","","","Active"),
Member("=IF(C23=\"\",\"\",\"THH\"&TEXT(A23,\"000\"))","","","Active"),
Member("=IF(C24=\"\",\"\",\"THH\"&TEXT(A24,\"000\"))","","","Active"),
Member("=IF(C25=\"\",\"\",\"THH\"&TEXT(A25,\"000\"))","","","Active"),
Member("=IF(C26=\"\",\"\",\"THH\"&TEXT(A26,\"000\"))","","","Active"),
Member("=IF(C27=\"\",\"\",\"THH\"&TEXT(A27,\"000\"))","","","Active"),
Member("=IF(C28=\"\",\"\",\"THH\"&TEXT(A28,\"000\"))","","","Active"),
Member("=IF(C29=\"\",\"\",\"THH\"&TEXT(A29,\"000\"))","","","Active"),
Member("=IF(C30=\"\",\"\",\"THH\"&TEXT(A30,\"000\"))","","","Active"),
Member("=IF(C31=\"\",\"\",\"THH\"&TEXT(A31,\"000\"))","","","Active"),
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(if(isAdmin) "Admin Dashboard" else "Member Dashboard", style=MaterialTheme.typography.headlineSmall)
            TextButton(onClick=logout){Text("Logout")}
        }
        Spacer(Modifier.height(12.dp))
        when(page) {
            "members" -> MembersPage(members)
            "contribution" -> ContributionPage()
            "loan" -> LoanPage()
            else -> HomePage(isAdmin)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
            Button({page="home"}){Text("Home")}
            Button({page="contribution"}){Text("Contribution")}
            Button({page="loan"}){Text("Loan")}
            if(isAdmin) Button({page="members"}){Text("Members")}
        }
    }
}

@Composable
fun HomePage(isAdmin:Boolean) {
    Text("Welcome to Helping Hands", style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Current Contribution")
            Text("₹600 / month", style=MaterialTheme.typography.headlineSmall)
            Text("Effective from August 2026")
        }
    }
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Late Penalty: ₹50 after 5th")
            Text("Reducing Interest: 0.9% monthly")
        }
    }
    if(isAdmin) {
        Spacer(Modifier.height(12.dp))
        Text("Admin tools: members, expenses, loans, reports and settings will be connected in the next build.")
    }
}

@Composable
fun ContributionPage() {
    Text("Contribution History", style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text("Jan–Jul 2026  ₹500")
    Text("Aug–Dec 2026  ₹600")
    Spacer(Modifier.height(8.dp))
    Text("Future changes will use an effective date so old months never change.")
    Spacer(Modifier.height(12.dp))
    Text("नियम: पुरानी दर कभी overwrite नहीं होगी; नई दर effective date के साथ जोड़ी जाएगी.")
}

@Composable
fun LoanPage() {
    Text("Loan & EMI", style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text("Reducing-balance interest: 0.9% monthly")
    Spacer(Modifier.height(8.dp))
    Text("EMI: reducing-balance method")
    Text("Monthly interest: 0.9%")
    Text("हर महीने Interest = Opening Outstanding × 0.9%")
    Text("Principal = EMI − Interest")
    Text("Closing = Opening − Principal")
}

@Composable
fun MembersPage(members:List<Member>) {
    Text("Members", style=MaterialTheme.typography.titleLarge)
    LazyColumn {
        items(members.size) { i ->
            ListItem(
                headlineContent={Text(members[i].name)},
                supportingContent={Text("${members[i].id} • ${members[i].mobile} • ${members[i].status}")}
            )
            HorizontalDivider()
        }
    }
}
