/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

#include "Plugin.h"

namespace qpid {

namespace {
// This is a single threaded singleton implementation so
// it is important to be sure that the first use of this
// singleton is when the program is still single threaded
Plugin::Plugins& thePlugins() {
    static Plugin::Plugins plugins;

    return plugins;
}
}

Plugin::Plugin() {
    // Register myself.
    thePlugins().push_back(this);
}

Plugin::~Plugin() {}

Options*  Plugin::getOptions() { return 0; }

const Plugin::Plugins& Plugin::getPlugins() {
    return thePlugins();
}

} // namespace qpid
