/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, Red Hat Inc.
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.debugger.linux.riscv32;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.riscv32.*;
import sun.jvm.hotspot.debugger.linux.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.debugger.cdbg.basic.*;

final public class LinuxRISCV32CFrame extends BasicCFrame {
   private static final int C_FRAME_LINK_OFFSET        = -2;
   private static final int C_FRAME_RETURN_ADDR_OFFSET = -1;

   public LinuxRISCV32CFrame(LinuxDebugger dbg, Address fp, Address pc) {
      super(dbg.getCDebugger());
      this.fp = fp;
      this.pc = pc;
      this.dbg = dbg;
   }

   // override base class impl to avoid ELF parsing
   public ClosestSymbol closestSymbolToPC() {
      // try native lookup in debugger.
      return dbg.lookup(dbg.getAddressValue(pc()));
   }

   public Address pc() {
      return pc;
   }

   public Address localVariableBase() {
      return fp;
   }

   public CFrame sender(ThreadProxy thread) {
      RISCV32ThreadContext context = (RISCV32ThreadContext) thread.getContext();
      Address rsp = context.getRegisterAsAddress(RISCV32ThreadContext.SP);

      if ((fp == null) || fp.lessThan(rsp)) {
        return null;
      }

      // Check alignment of fp
      if (dbg.getAddressValue(fp) % (2 * ADDRESS_SIZE) != 0) {
        return null;
      }

      Address nextFP = fp.getAddressAt(C_FRAME_LINK_OFFSET * ADDRESS_SIZE);
      if (nextFP == null || nextFP.lessThanOrEqual(fp)) {
        return null;
      }
      Address nextPC  = fp.getAddressAt(C_FRAME_RETURN_ADDR_OFFSET * ADDRESS_SIZE);
      if (nextPC == null) {
        return null;
      }
      return new LinuxRISCV32CFrame(dbg, nextFP, nextPC);
   }

   // package/class internals only
   private static final int ADDRESS_SIZE = 8;
   private Address pc;
   private Address sp;
   private Address fp;
   private LinuxDebugger dbg;
}
