package com.anontracker.personal
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="transactions", indices=[Index(value=["reference"], unique=true)])
data class IncomeTransaction(@PrimaryKey(autoGenerate=true) val id:Long=0,val amount:Double,val payer:String,val reference:String,val note:String="",val receivedAt:Long,val status:String="CONFIRMED",val source:String="MANUAL")
@Dao interface IncomeDao {
 @Query("SELECT * FROM transactions ORDER BY receivedAt DESC") fun observeAll():Flow<List<IncomeTransaction>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insert(item:IncomeTransaction):Long
 @Update suspend fun update(item:IncomeTransaction)
}
@Database(entities=[IncomeTransaction::class],version=1,exportSchema=false)
abstract class TrackerDb:RoomDatabase(){
 abstract fun incomeDao():IncomeDao
 companion object { @Volatile private var instance:TrackerDb?=null
 fun get(context:Context):TrackerDb=instance?: synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,TrackerDb::class.java,"anon_tracker.db").build().also{instance=it}}
 }
}
