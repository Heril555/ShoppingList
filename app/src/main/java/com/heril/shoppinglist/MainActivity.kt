package com.heril.shoppinglist

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.heril.shoppinglist.ui.theme.ShoppingListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppingListTheme {
                Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
                    Navigation()
                }
            }
        }
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewModel: LocationViewModel = viewModel()
    val context = LocalContext.current
    val locationUtils = LocationUtilities(context)

    NavHost(navController = navController, startDestination = "shoppinglistscreen") {
        composable("shoppinglistscreen") {
            shoppingList(
                locationUtils = locationUtils,
                viewModel = viewModel,
                navController = navController,
                context = context,
                address = viewModel.address.value.firstOrNull()?.formatted_address ?: "No Address"
            )
        }
        dialog("locationscreen") { backstack ->
            viewModel.location.value?.let { it1 ->
                LocationSelectionScreen(location = it1, onLocationSelected = {locationdata->
                    viewModel.fetchAddress("${locationdata.latitude},${locationdata.longitude}")
                    navController.popBackStack()
                })
            }
        }
    }
}

data class ShoppingItem(val id:Int,
                        var name:String,
                        var quantity:Int,
                        var isEditing:Boolean=false,
                        var address: String = ""
    )

@Composable
fun shoppingList(
    locationUtils: LocationUtilities,
    viewModel: LocationViewModel,
    navController: NavController,
    context: Context,
    address: String
){

    var sItems by remember{ mutableStateOf(listOf<ShoppingItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("") }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if(permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ){
                // I HAVE ACCESS to location
                locationUtils.requestLocationUpdates(viewModel)
            }else{
                //Ask for permission
                val rationaleRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                if(rationaleRequired){
                    Toast.makeText(context,
                        "Location Permission is required for this feature to work",
                        Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(context,
                        "Location Permission is required Please enable it in Android Settings",
                        Toast.LENGTH_LONG).show()
                }
            }
        })

    if (showDialog){
        AlertDialog(onDismissRequest = { showDialog=false },
            confirmButton = {
                Row(
                    modifier= Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Button(onClick = {
                        if(itemName.isNotBlank()){
                            val newItem=ShoppingItem(
                                id=sItems.size+1,
                                name=itemName,
                                quantity=itemQuantity.toInt(),
                                address = address
                            )
                            sItems += newItem
                            showDialog=false
                            itemName="" // Reset OutlinedTextField value
                        }

                    }) {
                        Text("Add")
                    }
                    Button(onClick = { showDialog=false }) {
                        Text("Cancel")
                    }
                }
            },
            title={ Text("Add Shopping Item")},
            text = {
                Column {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange ={ itemName=it },
                        singleLine = true,
                        placeholder = { Text("Item Name") }
                        )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = itemQuantity,
                        onValueChange ={ itemQuantity=it },
                        singleLine = true,
                        placeholder = { Text("Item Quantity") }
                        )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if(locationUtils.hasLocationPermission(context)){
                            locationUtils.requestLocationUpdates(viewModel)
                            navController.navigate("locationscreen"){
                                this.launchSingleTop
                            }
                        }else{
                            requestPermissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                    }){
                        Text("Address")
                    }
                }
            }
            )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { showDialog=true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Add Item")
        }
        LazyColumn(
            modifier= Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(sItems){
                var item=it
                if(item.isEditing){
                    shoppingItemEditor(
                        item,
                        onSaveClick = { updatedItem ->
                            sItems = sItems.map { if (it.id == updatedItem.id) updatedItem else it }
                        })
                }else{
                    shoppingListItem(
                        item,
                        onEditClick = { editedItem -> sItems = sItems.map { if (it.id == editedItem.id) editedItem.copy(isEditing = true) else it } },
                        onDeleteClick = { deletedItem -> sItems = sItems - deletedItem }
                    )
                }
            }
        }
    }
}

@Composable
fun shoppingListItem(
    item:ShoppingItem,
    onEditClick: (ShoppingItem) -> Unit, // Passing the item as a parameter
    onDeleteClick: (ShoppingItem) -> Unit // Passing the item as a parameter
){
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    2.dp,
                    Color(0XFF018786)
                ),// Or Color(hexadecimal)
                shape = RoundedCornerShape(20)
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Column(
            modifier = Modifier
                .weight(1f) // Allow content to take available space
                .padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Qty: ${item.quantity}")
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location Icon")
                Text(text = item.address)
            }
        }

        Row(
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically) // Align buttons vertically
        ) {
            IconButton(onClick = { onEditClick(item) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Button")
            }
            IconButton(onClick = { onDeleteClick(item) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Edit Button")
            }
        }
    }
}
@Composable
fun shoppingItemEditor(
    item:ShoppingItem,
    onSaveClick: (ShoppingItem) -> Unit // Passing a lambda function to handle save click
     ){
    var editedName by remember { mutableStateOf(item.name) }
    var editedQuantity by remember { mutableStateOf(item.quantity.toString()) }
    var editedAddress by remember { mutableStateOf(item.address) }
    //var isEditing by remember { mutableStateOf(item.isEditing) }

    Row(modifier = Modifier
        .fillMaxWidth()
        .background(Color.Gray)
        .padding(8.dp),
        horizontalArrangement=Arrangement.SpaceEvenly){
        Column {
            BasicTextField(
                value = editedName ,
                onValueChange = { editedName=it },
                singleLine = true,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp)
            )
            BasicTextField(
                value = editedQuantity ,
                onValueChange = { editedQuantity=it },
                singleLine = true,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp)
            )
        }
        Button(
            onClick = {
                val updatedItem = item.copy(
                    name = editedName,
                    quantity = editedQuantity.toIntOrNull() ?: 1,
                    address = editedAddress,
                    isEditing = false
                )
                onSaveClick(updatedItem)
            }
        ) {
            Text("Save")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun shoppingListPreview() {
//    //shoppingList()
//}