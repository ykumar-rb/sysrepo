/**
 * @file javaApplicationChangesExample.java
 * @author Mislav Novakovic <mislav.novakovic@sartura.hr>
 * @brief An example for Java bindings.
 *
 * @copyright
 * Copyright 2016 Deutsche Telekom AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.Scanner;

class Print {
	public void print_value(Val value) {
		System.out.print(value.xpath() + " ");

		if (value.type() == sr_type_t.SR_CONTAINER_T)
			System.out.println( "(container)" );
		else if (value.type() == sr_type_t.SR_CONTAINER_PRESENCE_T)
			System.out.println( "(container)" );
		else if (value.type() == sr_type_t.SR_LIST_T)
			System.out.println( "(list instance)" );
		else if (value.type() == sr_type_t.SR_STRING_T)
			System.out.println( "= " + value.data().get_string() );
		else if (value.type() == sr_type_t.SR_BOOL_T)
			if (value.data().get_bool())
				System.out.println( "= true" );
			else
				System.out.println( "= false" );
		else if (value.type() == sr_type_t.SR_ENUM_T)
			System.out.println( "= " + value.data().get_enum() );
		else if (value.type() == sr_type_t.SR_UINT8_T)
			System.out.println( "= " + value.data().get_uint8() );
		else if (value.type() == sr_type_t.SR_UINT16_T)
			System.out.println( "= " + value.data().get_uint16() );
		else if (value.type() == sr_type_t.SR_UINT32_T)
			System.out.println( "= " + value.data().get_uint32() );
		else if (value.type() == sr_type_t.SR_UINT64_T)
			System.out.println( "= " + value.data().get_uint64() );
		else if (value.type() == sr_type_t.SR_INT8_T)
			System.out.println( "= " + value.data().get_int8() );
		else if (value.type() == sr_type_t.SR_INT16_T)
			System.out.println( "= " + value.data().get_int16() );
		else if (value.type() == sr_type_t.SR_INT32_T)
			System.out.println( "= " + value.data().get_int32() );
		else if (value.type() == sr_type_t.SR_INT64_T)
			System.out.println( "= " + value.data().get_int64() );
		else if (value.type() == sr_type_t.SR_IDENTITYREF_T)
			System.out.println( "= " + value.data().get_identityref() );
		else if (value.type() == sr_type_t.SR_BITS_T)
			System.out.println( "= " + value.data().get_bits() );
		else if (value.type() == sr_type_t.SR_BINARY_T)
			System.out.println( "= " + value.data().get_binary() );
		else
			System.out.println( "(unprintable)");
	}

	public void change(sr_change_oper_t op, Val old_val, Val new_val) {
		if (op == sr_change_oper_t.SR_OP_CREATED) {
			System.out.print("CREATED: ");
			print_value(new_val);
		} else if (op == sr_change_oper_t.SR_OP_DELETED) {
			System.out.print("DELETED: ");
			print_value(old_val);
		} else if (op == sr_change_oper_t.SR_OP_MODIFIED) {
			System.out.print("MODIFIED: ");
			System.out.print("old value");
			print_value(old_val);
			System.out.print("new value");
			print_value(new_val);
		} else if (op == sr_change_oper_t.SR_OP_MOVED) {
			System.out.print( "MOVED: " + new_val.xpath() + " after " + old_val.xpath());
		}
	}

	public void current_config(Session session, String module_name) {
		String select_xpath = "/" + module_name + ":*//*";

		Vals values = session.get_items(select_xpath);

		for (int i = 0; i < values.val_cnt(); i++) {
			print_value(values.val(i));
		}
	}
}

class My_Callback extends Callback {
	public void module_change(Session sess, String module_name, sr_notif_event_t event, SWIGTYPE_p_void private_ctx) {
		System.out.println("\n\n ========== CONFIG HAS CHANGED, CURRENT RUNNING CONFIG: ==========\n");

		try {
			Print print = new Print();
			print.current_config(sess, module_name);

			System.out.println("\n\n ========== CONFIG HAS CHANGED, CURRENT RUNNING CONFIG: ==========\n");

			String change_path = "/" + module_name + ":*";

			Subscribe subscribe = new Subscribe(sess);
			Iter_Change it = subscribe.get_changes_iter(change_path);

			while (true) {
				Change change = subscribe.get_change_next(it);
				if (change == null)
					break;
				print.change(change.oper(), change.old_val(), change.new_val());
			}

			System.out.println("\n\n ========== END OF CHANGES =======================================\n");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}

public class javaApplicationChangesExample {
	static {
		System.loadLibrary("libsysrepoJava");
	}
	public static void main(String argv[]) {
		try {
			String module_name = "ietf-interfaces";

			// connect to sysrepo
			Connection conn = new Connection("example_application");

			// start session
			Session sess = new Session(conn);

			// subscribe for changes in running config
			Subscribe subscribe = new Subscribe(sess);

			My_Callback cb = new My_Callback();
			subscribe.module_change_subscribe(module_name, cb);

			System.out.println("\n\n ========== READING STARTUP CONFIG: ==========\n");

			try {
				Print print = new Print();
				print.current_config(sess, module_name);
			} catch (Exception e) {
				System.out.println(e);
			}

			boolean repeat = true;
			Scanner keyboard = new Scanner(System.in);
			while (repeat) {
				repeat = keyboard.nextBoolean();
			}

			System.out.println("Application exit requested, exiting.\n");

		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
